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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class RemoveExplicitTypeArgumentsInspection : IntentionBasedInspection<KtTypeArgumentList>(RemoveExplicitTypeArgumentsIntention::class) {
    override fun problemHighlightType(element: KtTypeArgumentList): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveExplicitTypeArgumentsIntention : SelfTargetingOffsetIndependentIntention<KtTypeArgumentList>(KtTypeArgumentList::class.java, "Remove explicit type arguments") {
    companion object {

        private fun KtCallExpression.argumentTypesDeducedFromReturnType(context: BindingContext): Boolean {
            val resolvedCall = getResolvedCall(context) ?: return false
            val typeParameters = resolvedCall.candidateDescriptor.typeParameters
            if (typeParameters.isEmpty()) return true
            val returnType = resolvedCall.candidateDescriptor.returnType ?: return false
            return returnType.arguments.map { it.type }.containsAll(typeParameters.map { it.defaultType })
        }

        private fun KtCallExpression.hasExplicitExpectedType(context: BindingContext): Boolean {
            // todo Check with expected type for other expressions
            // If always use expected type from trace there is a problem with nested calls:
            // the expression type for them can depend on their explicit type arguments (via outer call),
            // therefore we should resolve outer call with erased type arguments for inner call
            val parent = parent
            return when (parent) {
                is KtProperty -> parent.initializer == this && parent.typeReference != null
                is KtDeclarationWithBody -> parent.bodyExpression == this
                is KtReturnExpression -> true
                is KtValueArgument -> (parent.parent.parent as? KtCallExpression)?.let {
                    it.typeArgumentList != null ||
                    it.hasExplicitExpectedType(context) && it.argumentTypesDeducedFromReturnType(context)
                }?: false
                else -> false
            }
        }

        fun isApplicableTo(element: KtTypeArgumentList, approximateFlexible: Boolean): Boolean {
            val callExpression = element.parent as? KtCallExpression ?: return false
            if (callExpression.typeArguments.isEmpty()) return false

            val resolutionFacade = callExpression.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(callExpression, BodyResolveMode.PARTIAL)
            val originalCall = callExpression.getResolvedCall(bindingContext) ?: return false

            val (contextExpression, expectedType) = findContextToAnalyze(callExpression, bindingContext)
            val resolutionScope = contextExpression.getResolutionScope(bindingContext, resolutionFacade)

            val key = Key<Unit>("RemoveExplicitTypeArgumentsIntention")
            callExpression.putCopyableUserData(key, Unit)
            val expressionToAnalyze = contextExpression.copied()
            callExpression.putCopyableUserData(key, null)

            val newCallExpression = expressionToAnalyze.findDescendantOfType<KtCallExpression> { it.getCopyableUserData(key) != null }!!
            newCallExpression.typeArgumentList!!.delete()

            val newBindingContext = expressionToAnalyze.analyzeInContext(
                    resolutionScope,
                    contextExpression,
                    trace = DelegatingBindingTrace(bindingContext, "Temporary trace"),
                    dataFlowInfo = bindingContext.getDataFlowInfoBefore(contextExpression),
                    expectedType = expectedType ?: TypeUtils.NO_EXPECTED_TYPE,
                    isStatement = expectedType == null
            )

            val newCall = newCallExpression.getResolvedCall(newBindingContext) ?: return false

            val args = originalCall.typeArguments
            val newArgs = newCall.typeArguments

            fun equalTypes(type1: KotlinType, type2: KotlinType): Boolean {
                return if (approximateFlexible) {
                    KotlinTypeChecker.DEFAULT.equalTypes(type1, type2)
                }
                else {
                    type1 == type2
                }
            }

            return args.size == newArgs.size && args.values.zip(newArgs.values).all { (argType, newArgType) ->
                equalTypes(argType, newArgType)
            }
        }

        private fun findContextToAnalyze(expression: KtExpression, bindingContext: BindingContext): Pair<KtExpression, KotlinType?> {
            for (element in expression.parentsWithSelf) {
                if (element !is KtExpression) continue

                if (element.getQualifiedExpressionForSelector() != null) continue
                if (!element.isUsedAsExpression(bindingContext)) return element to null

                val parent = element.parent
                when (parent) {
                    is KtNamedFunction -> {
                        val expectedType = if (element == parent.bodyExpression && !parent.hasBlockBody() && parent.hasDeclaredReturnType())
                            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent] as? FunctionDescriptor)?.returnType
                        else
                            null
                        return element to expectedType
                    }

                    is KtVariableDeclaration -> {
                        val expectedType = if (element == parent.initializer && parent.typeReference != null)
                            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent] as? ValueDescriptor)?.type
                        else
                            null
                        return element to expectedType
                    }

                    is KtParameter -> {
                        val expectedType = if (element == parent.defaultValue)
                            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent] as? ValueDescriptor)?.type
                        else
                            null
                        return element to expectedType
                    }

                    is KtPropertyAccessor -> {
                        val property = parent.parent as KtProperty
                        val expectedType = when {
                            element != parent.bodyExpression || parent.hasBlockBody() -> null
                            parent.isSetter -> parent.builtIns.unitType
                            property.typeReference == null -> null
                            else -> (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent] as? FunctionDescriptor)?.returnType
                        }
                        return element to expectedType
                    }
                }
            }

            return expression to null
        }
    }

    override fun isApplicableTo(element: KtTypeArgumentList): Boolean {
        return isApplicableTo(element, approximateFlexible = false)
    }

    override fun applyTo(element: KtTypeArgumentList, editor: Editor?) {
        element.delete()
    }
}