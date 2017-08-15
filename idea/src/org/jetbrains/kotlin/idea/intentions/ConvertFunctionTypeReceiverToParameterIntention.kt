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
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.usagesSearch.searchReferencesOrMethodReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getArgumentByParameterIndex
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ConvertFunctionTypeReceiverToParameterIntention : SelfTargetingRangeIntention<KtTypeReference>(
        KtTypeReference::class.java,
        "Convert function type receiver to parameter"
) {
    class ConversionData(
            val functionParameterIndex: Int,
            val lambdaReceiverType: KotlinType,
            val function: KtFunction
    ) {
        val functionDescriptor by lazy { function.resolveToDescriptor() as FunctionDescriptor }
    }

    class FunctionDefinitionInfo(element: KtFunction) : AbstractProcessableUsageInfo<KtFunction, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val function = element ?: return
            val functionParameter = function.valueParameters.getOrNull(data.functionParameterIndex) ?: return
            val functionType = functionParameter.typeReference?.typeElement as? KtFunctionType ?: return
            val functionTypeParameterList = functionType.parameterList ?: return
            val functionTypeReceiver = functionType.receiverTypeReference ?: return
            val parameterToAdd = KtPsiFactory(project).createFunctionTypeParameter(functionTypeReceiver)
            functionTypeParameterList.addParameterBefore(parameterToAdd, functionTypeParameterList.parameters.firstOrNull())
            functionType.setReceiverTypeReference(null)
        }
    }

    class ParameterCallInfo(element: KtCallExpression) : AbstractProcessableUsageInfo<KtCallExpression, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val callExpression = element ?: return
            val qualifiedExpression = callExpression.getQualifiedExpressionForSelector() ?: return
            val receiverExpression = qualifiedExpression.receiverExpression
            val argumentList = callExpression.getOrCreateValueArgumentList()
            argumentList.addArgumentBefore(KtPsiFactory(project).createArgument(receiverExpression), argumentList.arguments.firstOrNull())
            qualifiedExpression.replace(callExpression)
        }
    }

    class LambdaInfo(element: KtLambdaExpression) : AbstractProcessableUsageInfo<KtLambdaExpression, ConversionData>(element) {
        override fun process(data: ConversionData, elementsToShorten: MutableList<KtElement>) {
            val lambda = element?.functionLiteral ?: return
            val context = lambda.analyze(BodyResolveMode.PARTIAL)

            val psiFactory = KtPsiFactory(project)

            val validator = CollectingNameValidator(
                    lambda.valueParameters.mapNotNull { it.name },
                    NewDeclarationNameValidator(lambda.bodyExpression!!, null, NewDeclarationNameValidator.Target.VARIABLES)
            )
            val newParameterName = KotlinNameSuggester.suggestNamesByType(data.lambdaReceiverType, validator, "p").first()
            val newParameterRefExpression = psiFactory.createExpression(newParameterName)

            lambda.forEachDescendantOfType<KtThisExpression> {
                val thisTarget = context[BindingContext.REFERENCE_TARGET, it.instanceReference] ?: return@forEachDescendantOfType
                if (DescriptorToSourceUtilsIde.getAnyDeclaration(project, thisTarget) == lambda) {
                    it.replace(newParameterRefExpression)
                }
            }

            val lambdaParameterList = lambda.getOrCreateParameterList()
            val parameterToAdd = psiFactory.createLambdaParameterList(newParameterName).parameters.first()
            lambdaParameterList.addParameterBefore(parameterToAdd, lambdaParameterList.parameters.firstOrNull())
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

                        if (callable !is KtFunction) continue

                        if (!checkModifiable(callable)) {
                            val renderedCallable = RefactoringUIUtil.getDescription(callable, true).capitalize()
                            conflicts.putValue(callable, "Can't modify $renderedCallable")
                        }

                        for (ref in callable.searchReferencesOrMethodReferences()) {
                            if (ref !is KtSimpleReference<*>) continue
                            processExternalUsage(ref, usages)
                        }

                        usages += FunctionDefinitionInfo(callable)

                        processInternalUsages(callable, usages)
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

        private fun processExternalUsage(ref: KtSimpleReference<*>, usages: java.util.ArrayList<AbstractProcessableUsageInfo<*, ConversionData>>) {
            val callElement = ref.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return
            val context = callElement.analyze(BodyResolveMode.PARTIAL)
            val expressionToProcess = callElement
                                              .getArgumentByParameterIndex(data.functionParameterIndex, context)
                                              .singleOrNull()
                                              ?.getArgumentExpression()
                                              ?.let { KtPsiUtil.safeDeparenthesize(it) }
                                      ?: return
            if (expressionToProcess is KtLambdaExpression) {
                usages += LambdaInfo(expressionToProcess)
            }
        }

        private fun processInternalUsages(callable: KtFunction, usages: java.util.ArrayList<AbstractProcessableUsageInfo<*, ConversionData>>) {
            val body = when (callable) {
                is KtConstructor<*> -> callable.containingClassOrObject?.getBody()
                else -> callable.bodyExpression
            }
            if (body != null) {
                val functionParameter = callable.valueParameters.getOrNull(data.functionParameterIndex) ?: return
                for (ref in ReferencesSearch.search(functionParameter, LocalSearchScope(body))) {
                    val element = ref.element as? KtSimpleNameExpression ?: continue
                    val callExpression = element.getParentOfTypeAndBranch<KtCallExpression> { calleeExpression } ?: continue
                    usages += ParameterCallInfo(callExpression)
                }
            }
        }
    }

    private fun KtTypeReference.getConversionData(): ConversionData? {
        val functionTypeReceiver = parent as? KtFunctionTypeReceiver ?: return null
        val functionType = functionTypeReceiver.parent as? KtFunctionType ?: return null
        val lambdaReceiverType = functionType
                                         .getAbbreviatedTypeOrType(functionType.analyze(BodyResolveMode.PARTIAL))
                                         ?.getReceiverTypeFromFunctionType()
                                 ?: return null
        val containingParameter = (functionType.parent as? KtTypeReference)?.parent as? KtParameter ?: return null
        val ownerFunction = containingParameter.ownerFunction as? KtFunction ?: return null
        val functionParameterIndex = ownerFunction.valueParameters.indexOf(containingParameter)
        return ConversionData(functionParameterIndex, lambdaReceiverType, ownerFunction)
    }

    override fun startInWriteAction(): Boolean = false

    override fun applicabilityRange(element: KtTypeReference): TextRange? {
        val data = element.getConversionData() ?: return null

        val elementBefore = data.function.valueParameters[data.functionParameterIndex].typeReference!!.typeElement as KtFunctionType
        val elementAfter = elementBefore.copied().apply {
            parameterList!!.addParameterBefore(
                    KtPsiFactory(element).createFunctionTypeParameter(element),
                    parameterList!!.parameters.firstOrNull()
            )
            setReceiverTypeReference(null)
        }
        text = "Convert '${elementBefore.text}' to '${elementAfter.text}'"

        return element.textRange
    }

    override fun applyTo(element: KtTypeReference, editor: Editor?) {
        element.getConversionData()?.let { Converter(it).run() }
    }
}