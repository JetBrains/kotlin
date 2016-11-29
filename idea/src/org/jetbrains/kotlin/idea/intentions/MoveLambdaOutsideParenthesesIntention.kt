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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MoveLambdaOutsideParenthesesIntention : SelfTargetingIntention<KtCallExpression>(KtCallExpression::class.java, "Move lambda argument out of parentheses") {
    companion object {
        private fun getLambdaExpression(callExpression: KtCallExpression): KtLambdaExpression? {
            if (callExpression.lambdaArguments.isNotEmpty()) return null
            return callExpression.valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral()
        }

        fun canMove(element: KtCallExpression): Boolean {
            if (getLambdaExpression(element) == null) return false

            val callee = element.calleeExpression
            if (callee is KtNameReferenceExpression) {
                val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
                val targets = bindingContext[BindingContext.REFERENCE_TARGET, callee]?.let { listOf(it) }
                              ?: bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, callee]
                              ?: listOf()
                val candidates = targets.filterIsInstance<FunctionDescriptor>()
                // if there are functions among candidates but none of them have last function parameter then not show the intention
                if (candidates.isNotEmpty() && candidates.none {
                    val lastParameter = it.valueParameters.lastOrNull()
                    lastParameter != null && lastParameter.type.isFunctionType
                }) {
                    return false
                }
            }

            return true
        }

        fun moveFunctionLiteralOutsideParenthesesIfPossible(expression: KtLambdaExpression) {
            val call = ((expression.parent as? KtValueArgument)?.parent as? KtValueArgumentList)?.parent as? KtCallExpression ?: return
            if (canMove(call)) {
                call.moveFunctionLiteralOutsideParentheses()
            }
        }
    }

    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        if (!canMove(element)) return false

        val lambdaExpression = getLambdaExpression(element) ?: return false
        val argument = lambdaExpression.getStrictParentOfType<KtValueArgument>() ?: return false
        if (caretOffset < argument.startOffset) return false
        val bodyRange = lambdaExpression.bodyExpression?.textRange ?: return true
        return !bodyRange.containsInside(caretOffset)
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        element.moveFunctionLiteralOutsideParentheses()
    }
}
