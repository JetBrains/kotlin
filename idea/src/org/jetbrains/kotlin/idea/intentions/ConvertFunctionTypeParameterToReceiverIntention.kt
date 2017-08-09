/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.explicateReceiverOf
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.usagesSearch.searchReferencesOrMethodReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getArgumentByParameterIndex
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ConvertFunctionTypeParameterToReceiverIntention : SelfTargetingRangeIntention<KtTypeReference>(
        KtTypeReference::class.java,
        "Convert function type parameter to receiver"
) {
    class FunctionDefinitionInfo(element: KtFunction) : AbstractProcessableUsageInfo<KtFunction, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val function = element ?: return
            val functionParameter = function.valueParameters.getOrNull(data.functionParameterIndex) ?: return
            val functionType = functionParameter.typeReference?.typeElement as? KtFunctionType ?: return
            val functionTypeParameterList = functionType.parameterList ?: return
            val parameterToMove = functionTypeParameterList.parameters.getOrNull(data.typeParameterIndex) ?: return
            val typeReferenceToMove = parameterToMove.typeReference ?: return
            functionType.setReceiverTypeReference(typeReferenceToMove)
            functionTypeParameterList.removeParameter(parameterToMove)
        }
    }

    class ParameterCallInfo(element: KtCallExpression) : AbstractProcessableUsageInfo<KtCallExpression, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val callExpression = element ?: return
            val argumentList = callExpression.valueArgumentList ?: return
            val expressionToMove = argumentList.arguments.getOrNull(data.typeParameterIndex)?.getArgumentExpression() ?: return
            val callWithReceiver = KtPsiFactory(callExpression).createExpressionByPattern("$0.$1", expressionToMove, callExpression) as KtQualifiedExpression
            (callWithReceiver.selectorExpression as KtCallExpression).valueArgumentList!!.removeArgument(data.typeParameterIndex)
            callExpression.replace(callWithReceiver)
        }
    }

    class InternalReferencePassInfo(element: KtSimpleNameExpression) : AbstractProcessableUsageInfo<KtSimpleNameExpression, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val expression = element ?: return
            val lambdaType = data.lambdaType
            val validator = CollectingNameValidator()
            val parameterNames = lambdaType.arguments
                    .dropLast(1)
                    .map { KotlinNameSuggester.suggestNamesByType(it.type, validator, "p").first() }
            val receiver = parameterNames.getOrNull(data.typeParameterIndex) ?: return
            val arguments = parameterNames.filter { it != receiver }
            val adapterLambda = KtPsiFactory(expression).createLambdaExpression(
                    parameterNames.joinToString(),
                    "$receiver.${expression.text}(${arguments.joinToString()})"
            )
            expression.replaced(adapterLambda).let {
                MoveLambdaOutsideParenthesesIntention.moveFunctionLiteralOutsideParenthesesIfPossible(it)
            }
        }
    }

    class LambdaInfo(element: KtExpression) : AbstractProcessableUsageInfo<KtExpression, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val expression = element ?: return
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val psiFactory = KtPsiFactory(expression)

            if (expression is KtLambdaExpression || (expression !is KtSimpleNameExpression && expression !is KtCallableReferenceExpression)) {
                expression.forEachDescendantOfType<KtThisExpression> {
                    if (it.getLabelName() != null) return@forEachDescendantOfType
                    val descriptor = context[BindingContext.REFERENCE_TARGET, it.instanceReference] ?: return@forEachDescendantOfType
                    it.replace(psiFactory.createExpression(explicateReceiverOf(descriptor)))
                }
            }

            if (expression is KtLambdaExpression) {
                expression.valueParameters.getOrNull(data.typeParameterIndex)?.let { parameterToConvert ->
                    val thisRefExpr = psiFactory.createThisExpression()
                    for (ref in ReferencesSearch.search(parameterToConvert, LocalSearchScope(expression))) {
                        (ref.element as? KtSimpleNameExpression)?.replace(thisRefExpr)
                    }
                    val lambda = expression.functionLiteral
                    lambda.valueParameterList!!.removeParameter(parameterToConvert)
                    if (lambda.valueParameters.isEmpty()) {
                        lambda.arrow?.delete()
                    }
                }
                return
            }

            val originalLambdaTypes = data.lambdaType
            val originalParameterTypes = originalLambdaTypes.arguments.dropLast(1).map { it.type }

            val calleeText = when (expression) {
                is KtSimpleNameExpression -> expression.text
                is KtCallableReferenceExpression -> "(${expression.text})"
                else -> generateVariable(expression)
            }

            val parameterNameValidator = CollectingNameValidator(
                    if (expression !is KtCallableReferenceExpression) listOf(calleeText) else emptyList()
            )
            val parameterNamesWithReceiver = originalParameterTypes.mapIndexed { i, type ->
                if (i != data.typeParameterIndex) KotlinNameSuggester.suggestNamesByType(type, parameterNameValidator, "p").first() else "this"
            }
            val parameterNames = parameterNamesWithReceiver.filter { it != "this" }

            val body = psiFactory.createExpression(parameterNamesWithReceiver.joinToString(prefix = "$calleeText(", postfix = ")"))

            val replacingLambda = psiFactory.buildExpression {
                appendFixedText("{ ")
                appendFixedText(parameterNames.joinToString())
                appendFixedText(" -> ")
                appendExpression(body)
                appendFixedText(" }")
            } as KtLambdaExpression

            expression.replaced(replacingLambda).let {
                MoveLambdaOutsideParenthesesIntention.moveFunctionLiteralOutsideParenthesesIfPossible(it)
            }
        }

        private fun generateVariable(expression: KtExpression): String {
            var baseCallee = ""
            KotlinIntroduceVariableHandler.doRefactoring(project, null, expression, false, emptyList()) {
                baseCallee = it.name!!
            }
            return baseCallee
        }
    }

    private inner class Converter(
            private val data: ConversionData
    ) : CallableRefactoring<CallableDescriptor>(data.function.project, data.functionDescriptor, text) {
        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val callables = getAffectedCallables(project, descriptorsForChange)

            val conflicts = MultiMap<PsiElement, String>()

            val usages = ArrayList<AbstractProcessableUsageInfo<*, ConversionData>>()

            project.runSynchronouslyWithProgress("Looking for usages and conflicts...", true) {
                runReadAction {
                    val progressStep = 1.0/callables.size
                    for ((i, callable) in callables.withIndex()) {
                        ProgressManager.getInstance().progressIndicator.fraction = (i + 1) * progressStep

                        if (callable !is PsiNamedElement) continue

                        if (!checkModifiable(callable)) {
                            val renderedCallable = RefactoringUIUtil.getDescription(callable, true).capitalize()
                            conflicts.putValue(callable, "Can't modify $renderedCallable")
                        }

                        usageLoop@ for (ref in callable.searchReferencesOrMethodReferences()) {
                            val refElement = ref.element ?: continue
                            when (ref) {
                                is KtSimpleReference<*> -> processExternalUsage(conflicts, refElement, usages)
                                is KtReference -> continue@usageLoop
                                else -> {
                                    if (data.isFirstParameter) continue@usageLoop
                                    conflicts.putValue(
                                            refElement,
                                            "Can't replace non-Kotlin reference with call expression: " + StringUtil.htmlEmphasize(refElement.text)
                                    )
                                }
                            }
                        }

                        if (callable is KtFunction) {
                            usages += FunctionDefinitionInfo(callable)
                            processInternalUsages(callable, usages)
                        }
                    }
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(text) {
                    val elementsToShorten = ArrayList<KtElement>()
                    usages.forEach { it.process(data, elementsToShorten) }
                    ShortenReferences.DEFAULT.process(elementsToShorten)
                }
            }
        }

        private fun processExternalUsage(
                conflicts: MultiMap<PsiElement, String>,
                refElement: PsiElement,
                usages: ArrayList<AbstractProcessableUsageInfo<*, ConversionData>>
        ) {
            val callElement = refElement.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
            if (callElement != null) {
                val context = callElement.analyze(BodyResolveMode.PARTIAL)
                val expressionToProcess = getArgumentExpressionToProcess(callElement, context) ?: return

                if (!data.isFirstParameter
                    && callElement is KtConstructorDelegationCall
                    && expressionToProcess !is KtLambdaExpression
                    && expressionToProcess !is KtSimpleNameExpression
                    && expressionToProcess !is KtCallableReferenceExpression) {
                    conflicts.putValue(
                            expressionToProcess,
                            "Following expression won't be processed since refactoring can't preserve its semantics: ${expressionToProcess.text}"
                    )
                    return
                }

                if (!checkThisExpressionsAreExplicatable(conflicts, context, expressionToProcess)) return

                if (data.isFirstParameter && expressionToProcess !is KtLambdaExpression) return

                usages += LambdaInfo(expressionToProcess)
                return
            }

            if (data.isFirstParameter) return

            val callableReference = refElement.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference }
            if (callableReference != null) {
                conflicts.putValue(
                        refElement,
                        "Callable reference transformation is not supported: " + StringUtil.htmlEmphasize(callableReference.text)
                )
                return
            }
        }

        private fun getArgumentExpressionToProcess(callElement: KtCallElement, context: BindingContext): KtExpression? {
            return callElement
                    .getArgumentByParameterIndex(data.functionParameterIndex, context)
                    .singleOrNull()
                    ?.getArgumentExpression()
                    ?.let { KtPsiUtil.safeDeparenthesize(it) }
        }

        private fun checkThisExpressionsAreExplicatable(conflicts: MultiMap<PsiElement, String>, context: BindingContext, expressionToProcess: KtExpression): Boolean {
            for (thisExpr in expressionToProcess.collectDescendantsOfType<KtThisExpression>()) {
                if (thisExpr.getLabelName() != null) continue
                val descriptor = context[BindingContext.REFERENCE_TARGET, thisExpr.instanceReference] ?: continue
                if (explicateReceiverOf(descriptor) == "this") {
                    conflicts.putValue(
                            thisExpr,
                            "Following expression won't be processed since refactoring can't preserve its semantics: ${thisExpr.text}"
                    )
                    return false
                }
            }
            return true
        }

        private fun processInternalUsages(callable: KtFunction, usages: ArrayList<AbstractProcessableUsageInfo<*, ConversionData>>) {
            val body = when (callable) {
                is KtConstructor<*> -> callable.containingClassOrObject?.getBody()
                else -> callable.bodyExpression
            }
            if (body != null) {
                val functionParameter = callable.valueParameters.getOrNull(data.functionParameterIndex) ?: return
                for (ref in ReferencesSearch.search(functionParameter, LocalSearchScope(body))) {
                    val element = ref.element as? KtSimpleNameExpression ?: continue
                    val callExpression = element.getParentOfTypeAndBranch<KtCallExpression> { calleeExpression }
                    if (callExpression != null) {
                        usages += ParameterCallInfo(callExpression)
                    }
                    else if (!data.isFirstParameter) {
                        usages += InternalReferencePassInfo(element)
                    }
                }
            }
        }
    }

    class ConversionData(
            val typeParameterIndex: Int,
            val functionParameterIndex: Int,
            val lambdaType: KotlinType,
            val function: KtFunction
    ) {
        val isFirstParameter: Boolean get() = typeParameterIndex == 0
        val functionDescriptor by lazy { function.resolveToDescriptor() as FunctionDescriptor }
    }

    private fun KtTypeReference.getConversionData(): ConversionData? {
        val parameter = parent as? KtParameter ?: return null
        val functionType = parameter.getParentOfTypeAndBranch<KtFunctionType> { parameterList } ?: return null
        if (functionType.receiverTypeReference != null) return null
        val lambdaType = functionType.getAbbreviatedTypeOrType(functionType.analyze(BodyResolveMode.PARTIAL)) ?: return null
        val containingParameter = (functionType.parent as? KtTypeReference)?.parent as? KtParameter ?: return null
        val ownerFunction = containingParameter.ownerFunction as? KtFunction ?: return null
        val typeParameterIndex = functionType.parameters.indexOf(parameter)
        val functionParameterIndex = ownerFunction.valueParameters.indexOf(containingParameter)
        return ConversionData(typeParameterIndex, functionParameterIndex, lambdaType, ownerFunction)
    }

    override fun startInWriteAction(): Boolean = false

    override fun applicabilityRange(element: KtTypeReference): TextRange? {
        val data = element.getConversionData() ?: return null

        val elementBefore = data.function.valueParameters[data.functionParameterIndex].typeReference!!.typeElement as KtFunctionType
        val elementAfter = elementBefore.copied().apply {
            setReceiverTypeReference(element)
            parameterList!!.removeParameter(data.typeParameterIndex)
        }
        text = "Convert '${elementBefore.text}' to '${elementAfter.text}'"

        return element.textRange
    }

    override fun applyTo(element: KtTypeReference, editor: Editor?) {
        element.getConversionData()?.let { Converter(it).run() }
    }
}
