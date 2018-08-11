/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class SetterBackingFieldAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return propertyAccessorVisitor(fun(accessor) {
            if (!accessor.isSetter) return
            if (accessor.hasLowerVisibilityThanProperty()) return
            val bodyExpression = accessor.bodyExpression as? KtBlockExpression ?: return
            if (bodyExpression.findDescendantOfType<KtBinaryExpression> {
                    it.operationToken == KtTokens.EQ && it.left?.text == "field"
                } != null) return
            holder.registerProblem(
                accessor,
                "Setter backing field should be assigned",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                AssignBackingFieldFix()
            )
        })
    }
}

private class AssignBackingFieldFix : LocalQuickFix {
    override fun getName() = "Assign backing filed"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val setter = descriptor.psiElement as? KtPropertyAccessor ?: return
        val parameter = setter.valueParameters.firstOrNull() ?: return
        val bodyExpression = setter.bodyExpression as? KtBlockExpression ?: return
        setter.hasBlockBody()
        bodyExpression.addBefore(
            KtPsiFactory(setter).createExpression("field = ${parameter.text}"),
            bodyExpression.rBrace
        )
    }
}