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
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*

class ConvertBinaryExpressionWithDemorgansLawIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "DeMorgan Law") {
    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        val expr = element.parentsWithSelf.takeWhile { it is KtBinaryExpression }.last() as KtBinaryExpression

        text = when (expr.operationToken) {
            KtTokens.ANDAND -> "Replace '&&' with '||'"
            KtTokens.OROR -> "Replace '||' with '&&'"
            else -> return false
        }

        return splitBooleanSequence(expr) != null
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        applyTo(element)
    }

    fun applyTo(element: KtBinaryExpression) {
        val expr = element.parentsWithSelf.takeWhile { it is KtBinaryExpression }.last() as KtBinaryExpression

        val operatorText = when (expr.operationToken) {
            KtTokens.ANDAND -> KtTokens.OROR.value
            KtTokens.OROR -> KtTokens.ANDAND.value
            else -> throw IllegalArgumentException()
        }

        val operands = splitBooleanSequence(expr)!!.asReversed()

        val newExpression = KtPsiFactory(expr).buildExpression {
            appendExpressions(operands.map { it.negate() }, separator = operatorText)
        }

        val grandParentPrefix = expr.parent.parent as? KtPrefixExpression
        val negated = expr.parent is KtParenthesizedExpression &&
                      grandParentPrefix?.operationReference?.getReferencedNameElementType() == KtTokens.EXCL
        if (negated) {
            grandParentPrefix?.replace(newExpression)
        } else {
            expr.replace(newExpression.negate())
        }
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
