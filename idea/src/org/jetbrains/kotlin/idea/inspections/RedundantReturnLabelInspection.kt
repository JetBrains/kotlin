/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.returnExpressionVisitor

class RedundantReturnLabelInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        returnExpressionVisitor(fun(returnExpression) {
            val label = returnExpression.getTargetLabel() ?: return
            val function = returnExpression.getParentOfType<KtNamedFunction>(true, KtLambdaExpression::class.java) ?: return
            if (function.name == null) return
            val labelName = label.getReferencedName()
            holder.registerProblem(
                label,
                "Redundant '@$labelName'",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveReturnLabelFix(labelName)
            )
        })
}

private class RemoveReturnLabelFix(private val labelName: String) : LocalQuickFix {
    override fun getName() = "Remove redundant '@$labelName'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor.psiElement?.delete()
    }
}