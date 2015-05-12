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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class SimplifyNegatedBinaryExpressionInspection : IntentionBasedInspection<JetPrefixExpression>(SimplifyNegatedBinaryExpressionIntention())

public class SimplifyNegatedBinaryExpressionIntention : JetSelfTargetingRangeIntention<JetPrefixExpression>(javaClass(), "Simplify negated binary expression") {

    private fun IElementType.negate(): JetSingleValueToken? = when (this) {
        JetTokens.IN_KEYWORD -> JetTokens.NOT_IN
        JetTokens.NOT_IN -> JetTokens.IN_KEYWORD

        JetTokens.IS_KEYWORD -> JetTokens.NOT_IS
        JetTokens.NOT_IS -> JetTokens.IS_KEYWORD

        JetTokens.EQEQ -> JetTokens.EXCLEQ
        JetTokens.EXCLEQ -> JetTokens.EQEQ

        JetTokens.LT -> JetTokens.GTEQ
        JetTokens.GTEQ -> JetTokens.LT

        JetTokens.GT -> JetTokens.LTEQ
        JetTokens.LTEQ -> JetTokens.GT

        else -> null
    }

    override fun applicabilityRange(element: JetPrefixExpression): TextRange? {
        return if (isApplicableTo(element)) element.getOperationReference().getTextRange() else null
    }

    public fun isApplicableTo(element: JetPrefixExpression): Boolean {
        if (element.getOperationToken() != JetTokens.EXCL) return false

        val expression = JetPsiUtil.deparenthesize(element.getBaseExpression()) as? JetOperationExpression ?: return false
        when (expression) {
            is JetIsExpression -> { if (expression.getTypeReference() == null) return false }
            is JetBinaryExpression -> { if (expression.getLeft() == null || expression.getRight() == null) return false }
            else -> return false
        }

        val operation = expression.getOperationReference().getReferencedNameElementType() as? JetSingleValueToken ?: return false
        val negatedOperation = operation.negate() ?: return false

        setText("Simplify negated '${operation.getValue()}' expression to '${negatedOperation.getValue()}'")
        return true
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: JetPrefixExpression) {
        val expression = JetPsiUtil.deparenthesize(element.getBaseExpression())!!
        val operation = (expression as JetOperationExpression).getOperationReference().getReferencedNameElementType().negate()!!.getValue()

        val psiFactory = JetPsiFactory(expression)
        val newExpression = when (expression) {
            is JetIsExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.getLeftHandSide(), operation, expression.getTypeReference()!!)
            is JetBinaryExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.getLeft()!!, operation, expression.getRight()!!)
            else -> throw IllegalArgumentException()
        }
        element.replace(newExpression)
    }
}