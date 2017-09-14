/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class SuspiciousEqualsCombination : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.parent is KtBinaryExpression) return
                    val operands = expression.parseBinary()
                    val eqeq = operands.eqEqOperands.map { it.text }
                    val eqeqeq = operands.eqEqEqOperands.map { it.text }
                    if (eqeq.intersect(eqeqeq).isNotEmpty()) {
                        holder.registerProblem(expression, "Suspicious combination of == and ===",
                                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    }
                }
            }

    private fun KtBinaryExpression.parseBinary(pair: ComparisonOperands = ComparisonOperands()): ComparisonOperands {
        when (operationToken) {
            KtTokens.EQEQ, KtTokens.EXCLEQ -> {
                (left as? KtNameReferenceExpression)?.let(pair.eqEqOperands::add)
                (right as? KtNameReferenceExpression)?.let(pair.eqEqOperands::add)
            }
            KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> {
                (left as? KtNameReferenceExpression)?.let(pair.eqEqEqOperands::add)
                (right as? KtNameReferenceExpression)?.let(pair.eqEqEqOperands::add)
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

private data class ComparisonOperands(val eqEqOperands: MutableList<KtExpression> = mutableListOf(),
                                      val eqEqEqOperands: MutableList<KtExpression> = mutableListOf())
