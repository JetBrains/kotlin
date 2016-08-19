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

class SimplifyNegatedBinaryExpressionInspection : IntentionBasedInspection<KtPrefixExpression>(SimplifyNegatedBinaryExpressionIntention::class)

class SimplifyNegatedBinaryExpressionIntention : SelfTargetingRangeIntention<KtPrefixExpression>(KtPrefixExpression::class.java, "Simplify negated binary expression") {

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
        return if (isApplicableTo(element)) element.operationReference.textRange else null
    }

    fun isApplicableTo(element: KtPrefixExpression): Boolean {
        if (element.operationToken != KtTokens.EXCL) return false

        val expression = KtPsiUtil.deparenthesize(element.baseExpression) as? KtOperationExpression ?: return false
        when (expression) {
            is KtIsExpression -> { if (expression.typeReference == null) return false }
            is KtBinaryExpression -> { if (expression.left == null || expression.right == null) return false }
            else -> return false
        }

        val operation = expression.operationReference.getReferencedNameElementType() as? KtSingleValueToken ?: return false
        val negatedOperation = operation.negate() ?: return false

        text = "Simplify negated '${operation.value}' expression to '${negatedOperation.value}'"
        return true
    }

    override fun applyTo(element: KtPrefixExpression, editor: Editor?) {
        val expression = KtPsiUtil.deparenthesize(element.baseExpression)!!
        val operation = (expression as KtOperationExpression).operationReference.getReferencedNameElementType().negate()!!.value

        val psiFactory = KtPsiFactory(expression)
        val newExpression = when (expression) {
            is KtIsExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.leftHandSide, operation, expression.typeReference!!)
            is KtBinaryExpression -> psiFactory.createExpressionByPattern("$0 $1 $2", expression.left!!, operation, expression.right!!)
            else -> throw IllegalArgumentException()
        }
        element.replace(newExpression)
    }
}