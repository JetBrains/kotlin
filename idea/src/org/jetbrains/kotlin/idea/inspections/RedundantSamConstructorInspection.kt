/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.getImportingScopeChain
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

public class RedundantSamConstructorInspection : AbstractKotlinInspection() {
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

            private fun createQuickFix(expressions: List<KtCallExpression>): LocalQuickFix {
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
                if (samConstructorCalls.size() == 1) {
                    val single = samConstructorCalls.single()
                    val problemDescriptor = holder.manager.
                            createProblemDescriptor(single.calleeExpression!!,
                                                    single.typeArgumentList ?: single.calleeExpression!!,
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
                                                    ProblemHighlightType.WEAK_WARNING,
                                                    isOnTheFly)

                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }

    companion  object {
        public fun replaceSamConstructorCall(callExpression: KtCallExpression): KtExpression {
            val functionalArgument = callExpression.samConstructorValueArgument()?.getArgumentExpression()
                                     ?: throw AssertionError("SAM-constructor should have a FunctionLiteralExpression as single argument: ${callExpression.getElementTextWithContext()}")
            return callExpression.replace(functionalArgument) as KtExpression
        }

        private fun canBeReplaced(parentCall: KtCallExpression, callExpressions: List<KtCallExpression>): Boolean {
            val context = parentCall.analyze(BodyResolveMode.PARTIAL)

            val calleeExpression = parentCall.calleeExpression ?: return false
            val scope = context[BindingContext.LEXICAL_SCOPE, calleeExpression] ?: return false

            val originalCall = parentCall.getResolvedCall(context) ?: return false

            val dataFlow = context.getDataFlowInfo(parentCall)
            val callResolver = parentCall.getResolutionFacade().frontendService<CallResolver>()
            val newCall = CallWithNewArguments(originalCall.call, callExpressions)

            val qualifiedExpression = parentCall.getQualifiedExpressionForSelectorOrThis()
            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

            val resolutionResults = callResolver.resolveFunctionCall(BindingTraceContext(), scope, newCall, expectedType, dataFlow, false)

            if (!resolutionResults.isSuccess) return false

            val samAdapterOriginalDescriptor = SamCodegenUtil.getOriginalIfSamAdapter(resolutionResults.resultingDescriptor) ?: return false
            return samAdapterOriginalDescriptor.original == originalCall.resultingDescriptor.original
        }

        private class CallWithNewArguments(original: Call, val callExpressions: List<KtCallExpression>): DelegatingCall(original) {
            private val newArguments: List<ValueArgument>

            init {
                val factory = KtPsiFactory(callElement)
                newArguments = original.valueArguments.map { argument ->
                    argument.getArgumentExpression()
                            ?.check { callExpressions.contains(it) }
                            ?.let { factory.createArgument(replaceSamConstructorCall(it.copy() as KtCallExpression), argument.getArgumentName()?.asName) }
                    ?: argument
                }
            }

            override fun getValueArguments() = newArguments
        }

        public fun samConstructorCallsToBeConverted(functionCall: KtCallExpression): List<KtCallExpression> {
            if (functionCall.valueArguments.all { !canBeSamConstructorCall(it) }) {
                return emptyList()
            }

            val bindingContext = functionCall.analyze(BodyResolveMode.PARTIAL)
            val functionResolvedCall = functionCall.getResolvedCall(bindingContext) ?: return emptyList()
            if (!functionResolvedCall.isReallySuccess()) return emptyList()

            val samConstructorCalls = functionCall.valueArguments.map {
                (it.getArgumentExpression() as? KtCallExpression)
                        ?.check { it.getResolvedCall(bindingContext)?.resultingDescriptor?.original is SamConstructorDescriptor }
            }.filterNotNull()

            if (samConstructorCalls.isEmpty()) return emptyList()

            val originalFunctionDescriptor = functionResolvedCall.resultingDescriptor.original as? FunctionDescriptor ?: return emptyList()
            val containingClass = originalFunctionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            // SAM adapters for static functions
            for (staticFunWithSameName in containingClass.staticScope.getFunctions(functionResolvedCall.resultingDescriptor.name, NoLookupLocation.FROM_IDE)) {
                if (staticFunWithSameName is SamAdapterDescriptor<*>) {
                    if (isSamAdapterSuitableForCall(staticFunWithSameName, originalFunctionDescriptor, samConstructorCalls.size())) {
                        return samConstructorCalls.check { canBeReplaced(functionCall, it) } ?: emptyList()
                    }
                }
            }

            // SAM adapters for member functions
            val resolutionScope = functionCall.getResolutionScope(bindingContext, functionCall.getResolutionFacade()).getImportingScopeChain()
            val syntheticExtensions = resolutionScope.getSyntheticExtensionFunctions(
                    containingClass.defaultType.singletonList(),
                    functionResolvedCall.resultingDescriptor.name,
                    NoLookupLocation.FROM_IDE
            )
            for (syntheticExtension in syntheticExtensions) {
                val samAdapter = syntheticExtension as? SamAdapterExtensionFunctionDescriptor ?: continue
                if (isSamAdapterSuitableForCall(samAdapter, originalFunctionDescriptor, samConstructorCalls.size())) {
                    return samConstructorCalls.check { canBeReplaced(functionCall, it) } ?: emptyList()
                }
            }

            return emptyList()
        }

        private fun canBeSamConstructorCall(argument: KtValueArgument): Boolean {
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression !is KtCallExpression) return false

            return argumentExpression.samConstructorValueArgument() != null
        }

        private fun KtCallExpression.samConstructorValueArgument(): KtValueArgument? {
            return valueArguments.singleOrNull()?.check { it.getArgumentExpression() is KtFunctionLiteralExpression }
        }

        private fun isSamAdapterSuitableForCall(
                samAdapter: FunctionDescriptor,
                originalFunction: FunctionDescriptor,
                samConstructorsCount: Int
        ): Boolean {
            val samAdapterOriginalFunction = SamCodegenUtil.getOriginalIfSamAdapter(samAdapter)?.original
            if (samAdapterOriginalFunction != originalFunction) return false

            val parametersWithSamTypeCount = originalFunction.valueParameters.count {
                SingleAbstractMethodUtils.getFunctionTypeForSamType(it.type) != null
            }

            return parametersWithSamTypeCount == samConstructorsCount
        }
    }
}