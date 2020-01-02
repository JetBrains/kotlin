/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*

class ConvertBinaryExpressionWithDemorgansLawIntention :
    SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "DeMorgan Law") {
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
