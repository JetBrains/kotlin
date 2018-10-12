/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

class ReplaceArraysCopyOfWithCopyOfInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return simpleNameExpressionVisitor { simpleNameExpression ->
            if (simpleNameExpression.isArraysCopyOf()) {
                holder.registerProblem(
                    simpleNameExpression,
                    "Replace 'Arrays.copyOf' with 'copyOf'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceArraysCopyOfWithCopyOfQuickfix()
                )
            }
        }
    }
}

class ReplaceArraysCopyOfWithCopyOfQuickfix : LocalQuickFix {
    override fun getName() = "Replace 'Arrays.copyOf' with 'copyOf'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtSimpleNameExpression ?: return
        val callExpression = (element.parent as? KtCallExpression) ?: return
        val qualifiedExpression = (callExpression.parent as? KtDotQualifiedExpression) ?: return

        val args = callExpression.valueArguments.mapNotNull { it.getArgumentExpression() }.toTypedArray() ?: return
        if (args.size != 2) return

        qualifiedExpression.replace(KtPsiFactory(element).createExpressionByPattern("$0.copyOf($1)", *args))
    }
}

private fun KtSimpleNameExpression.isArraysCopyOf(): Boolean {
    val callExpression = (parent as? KtCallExpression) ?: return false
    if (callExpression.valueArguments.size != 2) return false
    if (callExpression.valueArguments.mapNotNull { it.getArgumentExpression() }.size != 2) return false

    return callExpression.isCalling(FqName("java.util.Arrays.copyOf"))
}