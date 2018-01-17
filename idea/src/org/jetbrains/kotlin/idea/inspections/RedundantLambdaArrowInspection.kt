/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.lambdaExpressionVisitor

class RedundantLambdaArrowInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return lambdaExpressionVisitor { lambdaExpression ->
            val functionLiteral = lambdaExpression.functionLiteral
            if (functionLiteral.valueParameters.isNotEmpty()) return@lambdaExpressionVisitor
            val arrow = functionLiteral.arrow ?: return@lambdaExpressionVisitor

            holder.registerProblem(
                    arrow,
                    "Redundant lambda arrow",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    DeleteFix())
        }
    }

    class DeleteFix : LocalQuickFix {
        override fun getFamilyName() = "Remove arrow"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            FileModificationService.getInstance().preparePsiElementForWrite(element)
            element.delete()
        }
    }
}