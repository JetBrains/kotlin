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

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.lineCount
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LiftReturnOrAssignmentInspection @JvmOverloads constructor(private val skipLongExpressions: Boolean = true) :
    AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        object : KtVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                val states = getState(expression, skipLongExpressions) ?: return
                states.forEach { state ->
                    registerProblem(
                        expression,
                        state.keyword,
                        state.isSerious,
                        when (state.liftType) {
                            LiftType.LIFT_RETURN_OUT -> LiftReturnOutFix(state.keyword.text)
                            LiftType.LIFT_ASSIGNMENT_OUT -> LiftAssignmentOutFix(state.keyword.text)
                        },
                        state.highlightElement,
                        state.highlightType
                    )
                }
            }

            private fun registerProblem(
                expression: KtExpression,
                keyword: PsiElement,
                isSerious: Boolean,
                fix: LocalQuickFix,
                highlightElement: PsiElement = keyword,
                highlightType: ProblemHighlightType = if (isSerious) GENERIC_ERROR_OR_WARNING else INFORMATION
            ) {
                val subject = if (fix is LiftReturnOutFix) "Return" else "Assignment"
                val verb = if (isSerious) "should" else "can"
                holder.registerProblemWithoutOfflineInformation(
                    expression,
                    "$subject $verb be lifted out of '${keyword.text}'",
                    isOnTheFly,
                    highlightType,
                    highlightElement.textRange?.shiftRight(-expression.startOffset),
                    fix
                )
            }

        }

    private class LiftReturnOutFix(private val keyword: String) : LocalQuickFix {
        override fun getName() = "Lift return out of '$keyword'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val replaced = BranchedFoldingUtils.foldToReturn(descriptor.psiElement as KtExpression)
            replaced.findExistingEditor()?.caretModel?.moveToOffset(replaced.startOffset)
        }
    }

    private class LiftAssignmentOutFix(private val keyword: String) : LocalQuickFix {
        override fun getName() = "Lift assignment out of '$keyword'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            BranchedFoldingUtils.foldToAssignment(descriptor.psiElement as KtExpression)
        }
    }

    companion object {
        private const val LINES_LIMIT = 15

        fun getState(expression: KtExpression, skipLongExpressions: Boolean) = when (expression) {
            is KtWhenExpression -> getStateForWhenOrTry(expression, expression.whenKeyword, skipLongExpressions)
            is KtIfExpression -> getStateForWhenOrTry(expression, expression.ifKeyword, skipLongExpressions)
            is KtTryExpression -> expression.tryKeyword?.let {
                getStateForWhenOrTry(expression, it, skipLongExpressions)
            }
            else -> null
        }

        private fun getStateForWhenOrTry(
            expression: KtExpression,
            keyword: PsiElement,
            skipLongExpressions: Boolean
        ): List<LiftState>? {
            if (skipLongExpressions && expression.lineCount() > LINES_LIMIT) return null
            if (expression.isElseIf()) return null

            val foldableReturns = BranchedFoldingUtils.getFoldableReturns(expression)
            if (foldableReturns?.isNotEmpty() == true) {
                val hasOtherReturns = expression.anyDescendantOfType<KtReturnExpression> { it !in foldableReturns }
                val isSerious = !hasOtherReturns && foldableReturns.size > 1
                return foldableReturns.map {
                    LiftState(keyword, isSerious, LiftType.LIFT_RETURN_OUT, it, INFORMATION)
                } + LiftState(keyword, isSerious, LiftType.LIFT_RETURN_OUT)
            }

            val assignmentNumber = BranchedFoldingUtils.getFoldableAssignmentNumber(expression)
            if (assignmentNumber > 0) {
                val isSerious = assignmentNumber > 1
                return listOf(LiftState(keyword, isSerious, LiftType.LIFT_ASSIGNMENT_OUT))
            }
            return null
        }

        enum class LiftType {
            LIFT_RETURN_OUT, LIFT_ASSIGNMENT_OUT
        }

        data class LiftState(
            val keyword: PsiElement,
            val isSerious: Boolean,
            val liftType: LiftType,
            val highlightElement: PsiElement = keyword,
            val highlightType: ProblemHighlightType = if (isSerious) GENERIC_ERROR_OR_WARNING else INFORMATION
        )
    }
}
