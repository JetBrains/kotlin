/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.codegen.SamCodegenUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticMemberFunctions
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticStaticFunctions
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.keysToMapExceptNulls

class RedundantSamConstructorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private fun createQuickFix(expression: KtCallExpression): LocalQuickFix {
                return object : LocalQuickFix {
                    override fun getName() = "Remove redundant SAM-constructor"
                    override fun getFamilyName() = name
                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        replaceSamConstructorCall(expression)
                    }
                }
            }

            private fun createQuickFix(expressions: Collection<KtCallExpression>): LocalQuickFix {
                return object : LocalQuickFix {
                    override fun getName() = "Remove redundant SAM-constructors"
                    override fun getFamilyName() = name
                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        for (callExpression in expressions) {
                            replaceSamConstructorCall(callExpression)
                        }
                    }
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                if (expression.valueArguments.isEmpty()) return

                val samConstructorCalls = samConstructorCallsToBeConverted(expression)
                if (samConstructorCalls.isEmpty()) return
                val single = samConstructorCalls.singleOrNull()
                if (single != null) {
                    val calleeExpression = single.calleeExpression ?: return
                    val problemDescriptor = holder.manager.
                            createProblemDescriptor(calleeExpression,
                                                    single.typeArgumentList ?: calleeExpression,
                                                    "Redundant SAM-constructor",
                                                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                                    isOnTheFly,
                                                    createQuickFix(single))

                    holder.registerProblem(problemDescriptor)
                }
                else {
                    val problemDescriptor = holder.manager.
                            createProblemDescriptor(expression.valueArgumentList!!,
                                                    "Redundant SAM-constructors",
                                                    createQuickFix(samConstructorCalls),
                                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                    isOnTheFly)

                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }

    companion  object {
        fun replaceSamConstructorCall(callExpression: KtCallExpression): KtLambdaExpression {
            val functionalArgument = callExpression.samConstructorValueArgument()?.getArgumentExpression()
                                     ?: throw AssertionError("SAM-constructor should have a FunctionLiteralExpression as single argument: ${callExpression.getElementTextWithContext()}")
            return callExpression.getQualifiedExpressionForSelectorOrThis().replace(functionalArgument) as KtLambdaExpression
        }

        private fun canBeReplaced(parentCall: KtCallExpression,
                                  samConstructorCallArgumentMap: Map<KtValueArgument, KtCallExpression>): Boolean {
            val context = parentCall.analyze(BodyResolveMode.PARTIAL)

            val calleeExpression = parentCall.calleeExpression ?: return false
            val scope = calleeExpression.getResolutionScope(context, calleeExpression.getResolutionFacade())

            val originalCall = parentCall.getResolvedCall(context) ?: return false

            val dataFlow = context.getDataFlowInfoBefore(parentCall)
            val callResolver = parentCall.getResolutionFacade().frontendService<CallResolver>()
            val newCall = CallWithConvertedArguments(originalCall.call, samConstructorCallArgumentMap)

            val qualifiedExpression = parentCall.getQualifiedExpressionForSelectorOrThis()
            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

            val resolutionResults = callResolver.resolveFunctionCall(BindingTraceContext(), scope, newCall, expectedType, dataFlow, false)

            if (!resolutionResults.isSuccess) return false

            val samAdapterOriginalDescriptor = SamCodegenUtil.getOriginalIfSamAdapter(resolutionResults.resultingDescriptor) ?: return false
            return samAdapterOriginalDescriptor.original == originalCall.resultingDescriptor.original
        }

        private class CallWithConvertedArguments(
                original: Call,
                val callArgumentMapToConvert: Map<KtValueArgument, KtCallExpression>
        ) : DelegatingCall(original) {
            private val newArguments: List<ValueArgument>

            init {
                val factory = KtPsiFactory(callElement)
                newArguments = original.valueArguments.map { argument ->
                    val call = callArgumentMapToConvert[argument]
                    val newExpression = call?.samConstructorValueArgument()?.getArgumentExpression() ?: return@map argument
                    factory.createArgument(newExpression, argument.getArgumentName()?.asName)
                }
            }

            override fun getValueArguments() = newArguments
        }

        fun samConstructorCallsToBeConverted(functionCall: KtCallExpression): Collection<KtCallExpression> {
            val valueArguments = functionCall.valueArguments
            if (valueArguments.all { !canBeSamConstructorCall(it) }) {
                return emptyList()
            }

            val bindingContext = functionCall.analyze(BodyResolveMode.PARTIAL)
            val functionResolvedCall = functionCall.getResolvedCall(bindingContext) ?: return emptyList()
            if (!functionResolvedCall.isReallySuccess()) return emptyList()

            val samConstructorCallArgumentMap = valueArguments.keysToMapExceptNulls { it.toCallExpression() }.filter {
                (_, call) -> call.getResolvedCall(bindingContext)?.resultingDescriptor?.original is SamConstructorDescriptor
            }

            if (samConstructorCallArgumentMap.isEmpty()) return emptyList()

            if (samConstructorCallArgumentMap.values.any {
                val samValueArgument = it.samConstructorValueArgument()
                val samConstructorName = (it.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameAsName()
                samValueArgument == null || samConstructorName == null ||
                samValueArgument.hasLabeledReturnPreventingConversion(samConstructorName)
            }) return emptyList()

            val originalFunctionDescriptor = functionResolvedCall.resultingDescriptor.original as? FunctionDescriptor ?: return emptyList()
            val containingClass = originalFunctionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            val syntheticScopes = functionCall.getResolutionFacade().getFrontendService(SyntheticScopes::class.java)

            // SAM adapters for static functions
            val contributedFunctions = syntheticScopes.collectSyntheticStaticFunctions(containingClass.staticScope)
            for (staticFunWithSameName in contributedFunctions) {
                if (staticFunWithSameName is SamAdapterDescriptor<*>) {
                    if (isSamAdapterSuitableForCall(staticFunWithSameName, originalFunctionDescriptor, samConstructorCallArgumentMap.size)) {
                        return samConstructorCallArgumentMap.takeIf { canBeReplaced(functionCall, it) }?.values ?: emptyList()
                    }
                }
            }

            // SAM adapters for member functions
            val syntheticExtensions = syntheticScopes.collectSyntheticMemberFunctions(
                    listOf(containingClass.defaultType),
                    functionResolvedCall.resultingDescriptor.name,
                    NoLookupLocation.FROM_IDE)
            for (syntheticExtension in syntheticExtensions) {
                val samAdapter = syntheticExtension as? SamAdapterExtensionFunctionDescriptor ?: continue
                if (isSamAdapterSuitableForCall(samAdapter, originalFunctionDescriptor, samConstructorCallArgumentMap.size)) {
                    return samConstructorCallArgumentMap.takeIf { canBeReplaced(functionCall, it) }?.values ?: emptyList()
                }
            }

            return emptyList()
        }

        private fun canBeSamConstructorCall(argument: KtValueArgument)
                = argument.toCallExpression()?.samConstructorValueArgument() != null

        private fun KtCallExpression.samConstructorValueArgument(): KtValueArgument? {
            return valueArguments.singleOrNull()?.takeIf { it.getArgumentExpression() is KtLambdaExpression }
        }

        private fun ValueArgument.toCallExpression(): KtCallExpression? {
            val argumentExpression = getArgumentExpression()
            return (if (argumentExpression is KtDotQualifiedExpression)
                argumentExpression.selectorExpression
            else
                argumentExpression) as? KtCallExpression
        }

        private fun isSamAdapterSuitableForCall(
                samAdapter: FunctionDescriptor,
                originalFunction: FunctionDescriptor,
                samConstructorsCount: Int
        ): Boolean {
            val samAdapterOriginalFunction = SamCodegenUtil.getOriginalIfSamAdapter(samAdapter)?.original
            if (samAdapterOriginalFunction != originalFunction) return false

            val parametersWithSamTypeCount = originalFunction.valueParameters.count { SingleAbstractMethodUtils.isSamType(it.type) }
            return parametersWithSamTypeCount == samConstructorsCount
        }

        private fun KtValueArgument.hasLabeledReturnPreventingConversion(samConstructorName: Name) =
                anyDescendantOfType<KtReturnExpression> { it.getLabelNameAsName() == samConstructorName }
    }
}
