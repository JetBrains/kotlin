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
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MoveLambdaOutsideParenthesesIntention : SelfTargetingIntention<KtCallExpression>(KtCallExpression::class.java, "Move lambda argument out of parentheses") {
    companion object {
        fun moveFunctionLiteralOutsideParenthesesIfPossible(expression: KtLambdaExpression) {
            val call = ((expression.parent as? KtValueArgument)?.parent as? KtValueArgumentList)?.parent as? KtCallExpression ?: return
            if (call.canMoveLambdaOutsideParentheses()) {
                call.moveFunctionLiteralOutsideParentheses()
            }
        }
    }

    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        if (!element.canMoveLambdaOutsideParentheses()) return false

        val lambdaExpression = element.getLastLambdaExpression() ?: return false
        val argument = lambdaExpression.getStrictParentOfType<KtValueArgument>() ?: return false
        if (caretOffset < argument.startOffset) return false
        val bodyRange = lambdaExpression.bodyExpression?.textRange ?: return true
        return !bodyRange.containsInside(caretOffset)
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        element.moveFunctionLiteralOutsideParentheses()
    }
}
