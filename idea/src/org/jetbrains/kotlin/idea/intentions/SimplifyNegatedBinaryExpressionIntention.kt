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
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

public class SimplifyNegatedBinaryExpressionInspection : IntentionBasedInspection<KtPrefixExpression>(SimplifyNegatedBinaryExpressionIntention())

public class SimplifyNegatedBinaryExpressionIntention : JetSelfTargetingRangeIntention<KtPrefixExpression>(javaClass(), "Simplify negated binary expression") {

    private fun IElementType.negate(): KtSingleValueToken? = when (this) {
        KtTokens.IN_KEYWORD -> KtTokens.NOT_IN
        KtTokens.NOT_IN -> KtTokens.IN_KEYWORD

        KtTokens.IS_KEYWORD -> KtTokens.NOT_IS
        KtTokens.NOT_IS -> KtTokens.IS_KEYWORD

        KtTokens.EQEQ -> KtTokens.EXCLEQ
        KtTokens.EXCLEQ -> KtTokens.EQEQ

        KtTokens.LT -> KtTokens.GTEQ
        KtTokens.GTEQ -> KtTokens.LT

        KtTokens.GT -> KtTokens.LTEQ
        KtTokens.LTEQ -> KtTokens.GT

        else -> null
    }

    override fun applicabilityRange(element: KtPrefixExpression): TextRange? {
        return if (isApplicableTo(element)) element.getOperationReference().getTextRange() else null
    }

    public fun isApplicableTo(element: KtPrefixExpression): Boolean {
        if (element.getOperationToken() != KtTokens.EXCL) return false

        val expression = KtPsiUtil.deparenthesize(element.getBaseExpression()) as? KtOperationExpression ?: return false
        when (expression) {
            is KtIsExpression -> { if (expression.getTypeReference() == null) return false }
            is KtBinaryExpression -> { if (expression.getLeft() == null || expression.getRight() == null) return false }
            else -> return false
        }

        val operation = expression.getOperationReference().getReferencedNameElementType() as? KtSingleValueToken ?: return false
        val negatedOperation = operation.negate() ?: return false

        setText("Simplify negated '${operation.getValue()}' expression to '${negatedOperation.getValue()}'")
        return true
    }

    override fun applyTo(element: KtPrefixExpression, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: KtPrefixExpression) {
        val expression = KtPsiUtil.deparenthesize(element.getBaseExpression())!!
        val operation = (expression as KtOperationExpression).getOperationReference().getReferencedNameElementType().negate()!!.getValue()

        val psiFactory = KtPsiFactory(expression)
        val newExpression = when (expression) {
            is KtIsExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.getLeftHandSide(), operation, expression.getTypeReference()!!)
            is KtBinaryExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.getLeft()!!, operation, expression.getRight()!!)
            else -> throw IllegalArgumentException()
        }
        element.replace(newExpression)
    }
}