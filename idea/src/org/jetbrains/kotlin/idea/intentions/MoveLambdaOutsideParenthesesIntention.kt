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
import org.jetbrains.kotlin.idea.util.psiModificationUtil.moveLambdaOutsideParentheses
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses

public class MoveLambdaOutsideParenthesesIntention : JetSelfTargetingIntention<JetCallExpression>(javaClass(), "Move lambda argument out of parentheses") {
    override fun isApplicableTo(element: JetCallExpression, caretOffset: Int): Boolean {
        val argument = element.getValueArgumentsInParentheses().lastOrNull() ?: return false
        val expression = argument.getArgumentExpression() ?: return false
        val functionLiteral = getFunctionLiteral(expression) ?: return false
        if (caretOffset < argument.asElement().startOffset) return false
        val bodyRange = functionLiteral.getBodyExpression()?.getTextRange() ?: return true
        return !bodyRange.containsInside(caretOffset)
    }

    private fun getFunctionLiteral(expression: JetExpression?): JetFunctionLiteral? {
        return when (expression) {
            is JetFunctionLiteralExpression -> expression.getFunctionLiteral()
            is JetLabeledExpression -> getFunctionLiteral(expression.getBaseExpression())
            else -> null
        }
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        element.moveLambdaOutsideParentheses()
    }
}
