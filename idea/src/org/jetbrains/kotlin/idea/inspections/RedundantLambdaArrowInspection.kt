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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.lambdaExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore

class RedundantLambdaArrowInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return lambdaExpressionVisitor { lambdaExpression ->
            val functionLiteral = lambdaExpression.functionLiteral
            val arrow = functionLiteral.arrow ?: return@lambdaExpressionVisitor
            val parameters = functionLiteral.valueParameters
            val singleParameter = parameters.singleOrNull()
            if (parameters.isNotEmpty() && singleParameter?.isSingleUnderscore != true) return@lambdaExpressionVisitor

            val startOffset = functionLiteral.startOffset
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    functionLiteral,
                    TextRange((singleParameter?.startOffset ?: arrow.startOffset) - startOffset, arrow.endOffset - startOffset),
                    "Redundant lambda arrow",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    DeleteFix()
                )
            )
        }
    }

    class DeleteFix : LocalQuickFix {
        override fun getFamilyName() = "Remove arrow"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as? KtFunctionLiteral ?: return
            FileModificationService.getInstance().preparePsiElementForWrite(element)
            element.valueParameterList?.delete()
            element.arrow?.delete()
        }
    }
}