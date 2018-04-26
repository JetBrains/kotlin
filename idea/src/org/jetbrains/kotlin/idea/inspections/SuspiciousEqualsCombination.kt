/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class SuspiciousEqualsCombination : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        binaryExpressionVisitor(fun(expression) {
            if (expression.parent is KtBinaryExpression) return
            val operands = expression.parseBinary()
            val eqeq = operands.eqEqOperands.map { it.text }
            val eqeqeq = operands.eqEqEqOperands.map { it.text }
            if (eqeq.intersect(eqeqeq).isNotEmpty()) {
                holder.registerProblem(
                    expression, "Suspicious combination of == and ===",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        })

    private fun KtBinaryExpression.parseBinary(pair: ComparisonOperands = ComparisonOperands()): ComparisonOperands {
        when (operationToken) {
            KtTokens.EQEQ, KtTokens.EXCLEQ -> {
                if (!left.isNullExpression() && !right.isNullExpression()) {
                    (left as? KtNameReferenceExpression)?.let(pair.eqEqOperands::add)
                    (right as? KtNameReferenceExpression)?.let(pair.eqEqOperands::add)
                }
            }
            KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> {
                if (!left.isNullExpression() && !right.isNullExpression()) {
                    (left as? KtNameReferenceExpression)?.let(pair.eqEqEqOperands::add)
                    (right as? KtNameReferenceExpression)?.let(pair.eqEqEqOperands::add)
                }
            }
            KtTokens.ANDAND, KtTokens.OROR -> {
                right?.parseExpression(pair)
                left?.parseExpression(pair)
            }
        }
        return pair
    }

    private fun KtExpression.parseExpression(pair: ComparisonOperands) {
        when (this) {
            is KtBinaryExpression -> parseBinary(pair)
            is KtParenthesizedExpression -> expression?.parseExpression(pair)
            is KtPrefixExpression -> if (operationToken == KtTokens.EXCL) baseExpression?.parseExpression(pair)
        }
    }
}

private data class ComparisonOperands(
    val eqEqOperands: MutableList<KtExpression> = mutableListOf(),
    val eqEqEqOperands: MutableList<KtExpression> = mutableListOf()
)
