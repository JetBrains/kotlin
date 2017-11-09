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

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfToWhenIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isOneLiner
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis

class CascadeIfInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitIfExpression(expression: KtIfExpression) {
                    super.visitIfExpression(expression)

                    val branches = expression.branches
                    if (branches.size <= 2) return
                    if (expression.isOneLiner()) return

                    if (branches.any {
                        it == null ||
                        it.lastBlockStatementOrThis() is KtIfExpression
                    }) return

                    if (expression.isElseIf()) return

                    if (expression.anyDescendantOfType<KtExpressionWithLabel> {
                        it is KtBreakExpression || it is KtContinueExpression
                    }) return

                    var current: KtIfExpression? = expression
                    while (current != null) {
                        val condition = current.condition
                        when (condition) {
                            is KtBinaryExpression -> when (condition.operationToken) {
                                KtTokens.ANDAND, KtTokens.OROR -> return
                            }
                            is KtUnaryExpression -> when (condition.operationToken) {
                                KtTokens.EXCL -> return
                            }
                        }
                        current = current.`else` as? KtIfExpression
                    }

                    holder.registerProblem(
                            expression.ifKeyword,
                            "Cascade if can be replaced with when",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            IntentionWrapper(IfToWhenIntention(), expression.containingKtFile)
                    )
                }
            }
}