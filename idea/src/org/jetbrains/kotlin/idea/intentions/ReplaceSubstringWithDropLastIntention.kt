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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class ReplaceSubstringWithDropLastIntention : ReplaceSubstringIntention("Replace 'substring' call with 'dropLast' call") {
    override fun applicabilityRangeInner(element: KtDotQualifiedExpression): TextRange? {
        val arguments = element.callExpression?.valueArguments ?: return null
        if (arguments.size != 2 || !element.isFirstArgumentZero()) return null

        val secondArgumentExpression = arguments[1].getArgumentExpression()

        if (secondArgumentExpression !is KtBinaryExpression) return null
        if (secondArgumentExpression.operationReference.getReferencedNameElementType() != KtTokens.MINUS) return null
        if (isLengthAccess(secondArgumentExpression.left, element.receiverExpression)) {
            return getTextRange(element)
        }

        return null
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val argument = element.callExpression!!.valueArguments[1].getArgumentExpression()!!
        val rightExpression = (argument as KtBinaryExpression).right!!

        element.replaceWith("$0.dropLast($1)", rightExpression)
    }

    private fun isLengthAccess(expression: KtExpression?, expectedReceiver: KtExpression): Boolean {
        return expression is KtDotQualifiedExpression
               && expression.selectorExpression.let { it is KtNameReferenceExpression && it.getReferencedName() == "length" }
               && expression.receiverExpression.evaluatesTo(expectedReceiver)
    }
}
