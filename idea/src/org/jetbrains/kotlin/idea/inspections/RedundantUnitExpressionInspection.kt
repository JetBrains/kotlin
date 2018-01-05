/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class RedundantUnitExpressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {
            if (KotlinBuiltIns.FQ_NAMES.unit.shortName() != (expression as? KtNameReferenceExpression)?.getReferencedNameAsName()) {
                return
            }

            val parent = expression.parent
            if (parent !is KtReturnExpression && parent !is KtBlockExpression) return

            // Do not report just 'Unit' in function literals (return@label Unit is OK even in literals)
            if (parent is KtBlockExpression && parent.getParentOfType<KtFunctionLiteral>(strict = true) != null) return

            holder.registerProblem(expression,
                                   "Redundant 'Unit'",
                                   ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                   RemoveRedundantUnitFix())
        })
    }
}

private class RemoveRedundantUnitFix : LocalQuickFix {
    override fun getName() = "Remove redundant 'Unit'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtReferenceExpression)?.delete()
    }
}
