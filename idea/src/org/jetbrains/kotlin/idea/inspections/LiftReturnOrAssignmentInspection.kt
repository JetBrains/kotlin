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
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class LiftReturnOrAssignmentInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                private fun visitIfOrWhen(expression: KtExpression, keyword: PsiElement) {
                    if (BranchedFoldingUtils.canFoldToReturn(expression)) {
                        holder.registerProblem(
                                keyword,
                                "Return can be lifted out of '${keyword.text}'",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                LiftReturnOutFix(keyword.text)
                        )
                    }
                    else if (BranchedFoldingUtils.canFoldToAssignment(expression)) {
                        holder.registerProblem(
                                keyword,
                                "Assignment can be lifted out of '${keyword.text}'",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                LiftAssignmentOutFix(keyword.text)
                        )
                    }
                }

                override fun visitIfExpression(expression: KtIfExpression) {
                    super.visitIfExpression(expression)
                    visitIfOrWhen(expression, expression.ifKeyword)
                }

                override fun visitWhenExpression(expression: KtWhenExpression) {
                    super.visitWhenExpression(expression)
                    visitIfOrWhen(expression, expression.whenKeyword)
                }
            }

    private class LiftReturnOutFix (private val keyword: String) : LocalQuickFix {
        override fun getName() = "Lift return out of '$keyword'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            BranchedFoldingUtils.foldToReturn(descriptor.psiElement.getParentOfType(true)!!)
        }
    }

    private class LiftAssignmentOutFix (private val keyword: String) : LocalQuickFix {
        override fun getName() = "Lift assignment out of '$keyword'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            BranchedFoldingUtils.foldToAssignment(descriptor.psiElement.getParentOfType(true)!!)
        }
    }
}
