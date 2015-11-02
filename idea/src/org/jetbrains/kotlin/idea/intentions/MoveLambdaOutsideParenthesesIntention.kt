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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.unpackFunctionLiteral
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class MoveLambdaOutsideParenthesesIntention : SelfTargetingIntention<KtCallExpression>(javaClass(), "Move lambda argument out of parentheses") {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        if (element.getFunctionLiteralArguments().isNotEmpty()) return false
        val argument = element.getValueArguments().lastOrNull() ?: return false
        val expression = argument.getArgumentExpression() ?: return false
        val functionLiteral = expression.unpackFunctionLiteral() ?: return false

        val callee = element.getCalleeExpression()
        if (callee is KtNameReferenceExpression) {
            val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
            val targets = bindingContext[BindingContext.REFERENCE_TARGET, callee]?.let { listOf(it) }
                          ?: bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, callee]
                          ?: listOf()
            val candidates = targets.filterIsInstance<FunctionDescriptor>()
            // if there are functions among candidates but none of them have last function parameter then not show the intention
            if (candidates.isNotEmpty() && candidates.none {
                val lastParameter = it.getValueParameters().lastOrNull()
                lastParameter != null && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(lastParameter.getType())
            }) {
                return false
            }
        }

        if (caretOffset < argument.asElement().startOffset) return false
        val bodyRange = functionLiteral.getBodyExpression()?.getTextRange() ?: return true
        return !bodyRange.containsInside(caretOffset)
    }

    override fun applyTo(element: KtCallExpression, editor: Editor) {
        element.moveFunctionLiteralOutsideParentheses()
    }

    fun applyTo(element: KtCallExpression) {
        element.moveFunctionLiteralOutsideParentheses()
    }
}
