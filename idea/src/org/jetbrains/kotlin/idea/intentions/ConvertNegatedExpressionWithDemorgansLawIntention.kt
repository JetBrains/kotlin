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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import java.util.*

class ConvertNegatedExpressionWithDemorgansLawIntention : SelfTargetingOffsetIndependentIntention<KtPrefixExpression>(KtPrefixExpression::class.java, "DeMorgan Law") {
    override fun isApplicableTo(element: KtPrefixExpression): Boolean {
        val prefixOperator = element.operationReference.getReferencedNameElementType()
        if (prefixOperator != KtTokens.EXCL) return false

        val parenthesizedExpression = element.baseExpression as? KtParenthesizedExpression
        val baseExpression = parenthesizedExpression?.expression as? KtBinaryExpression ?: return false

        when (baseExpression.operationToken) {
            KtTokens.ANDAND -> text = "Replace '&&' with '||'"
            KtTokens.OROR -> text = "Replace '||' with '&&'"
            else -> return false
        }

        return splitBooleanSequence(baseExpression) != null
    }

    override fun applyTo(element: KtPrefixExpression, editor: Editor?) {
        applyTo(element)
    }

    fun applyTo(element: KtPrefixExpression) {
        val parenthesizedExpression = element.baseExpression as KtParenthesizedExpression
        val baseExpression = parenthesizedExpression.expression as KtBinaryExpression

        val operatorText = when (baseExpression.operationToken) {
            KtTokens.ANDAND -> KtTokens.OROR.value
            KtTokens.OROR -> KtTokens.ANDAND.value
            else -> throw IllegalArgumentException()
        }

        val operands = splitBooleanSequence(baseExpression)!!.asReversed()

        val newExpression = KtPsiFactory(element).buildExpression {
            appendExpressions(operands.map { it.negate() }, separator = operatorText)
        }

        element.replace(newExpression)
    }

    private fun splitBooleanSequence(expression: KtBinaryExpression): List<KtExpression>? {
        val result = ArrayList<KtExpression>()
        val firstOperator = expression.operationToken

        var remainingExpression: KtExpression = expression
        while (true) {
            if (remainingExpression !is KtBinaryExpression) break

            val operation = remainingExpression.operationToken
            if (operation != KtTokens.ANDAND && operation != KtTokens.OROR) break

            if (operation != firstOperator) return null //Boolean sequence must be homogenous

            result.add(remainingExpression.right ?: return null)
            remainingExpression = remainingExpression.left ?: return null
        }

        result.add(remainingExpression)
        return result
    }

}
