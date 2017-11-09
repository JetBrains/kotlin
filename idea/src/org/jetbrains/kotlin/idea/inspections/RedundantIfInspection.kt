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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class RedundantIfInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)
                if (expression.condition == null) return
                val (redundancyType, branchType) = RedundancyType.of(expression)
                if (redundancyType == RedundancyType.NONE) return

                holder.registerProblem(expression,
                                       "Redundant 'if' statement",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       RemoveRedundantIf(redundancyType, branchType))
            }
        }
    }

    private sealed class BranchType {
        object Simple : BranchType()

        object Return : BranchType()

        class Assign(val lvalue: KtExpression) : BranchType() {
            override fun equals(other: Any?) = other is Assign && lvalue.text == other.lvalue.text

            override fun hashCode() = lvalue.text.hashCode()
        }
    }

    private enum class RedundancyType {
        NONE,
        THEN_TRUE,
        ELSE_TRUE;

        companion object {
            internal fun of(expression: KtIfExpression): Pair<RedundancyType, BranchType> {
                val (thenReturn, thenType) = expression.then.getBranchExpression() ?: return NONE to BranchType.Simple
                val (elseReturn, elseType) = expression.`else`.getBranchExpression() ?: return NONE to BranchType.Simple

                return when {
                    thenType != elseType -> NONE to BranchType.Simple
                    KtPsiUtil.isTrueConstant(thenReturn) && KtPsiUtil.isFalseConstant(elseReturn) -> THEN_TRUE to thenType
                    KtPsiUtil.isFalseConstant(thenReturn) && KtPsiUtil.isTrueConstant(elseReturn) -> ELSE_TRUE to thenType
                    else -> NONE to BranchType.Simple
                }
            }

            private fun KtExpression?.getBranchExpression(): Pair<KtExpression?, BranchType>? {
                return when (this) {
                    is KtReturnExpression -> returnedExpression to BranchType.Return
                    is KtBlockExpression -> statements.singleOrNull()?.getBranchExpression()
                    is KtBinaryExpression -> if (operationToken == KtTokens.EQ && left != null)
                        right to BranchType.Assign(left!!)
                    else
                        null
                    is KtExpression -> this to BranchType.Simple
                    else -> null
                }
            }
        }
    }

    private class RemoveRedundantIf(private val redundancyType: RedundancyType, private val branchType: BranchType) : LocalQuickFix {
        override fun getName() = "Remove redundant 'if' statement"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as KtIfExpression
            if (!CodeInsightUtil.preparePsiElementsForWrite(element)) return
            val condition = when (redundancyType) {
                RedundancyType.NONE -> return
                RedundancyType.THEN_TRUE -> element.condition!!
                RedundancyType.ELSE_TRUE -> element.condition!!.negate()
            }
            val factory = KtPsiFactory(element)
            element.replace(
                    when (branchType) {
                        is BranchType.Return -> factory.createExpressionByPattern("return $0", condition)
                        is BranchType.Assign -> factory.createExpressionByPattern("$0 = $1", branchType.lvalue, condition)
                        else -> condition
                    }
            )
        }
    }

}
