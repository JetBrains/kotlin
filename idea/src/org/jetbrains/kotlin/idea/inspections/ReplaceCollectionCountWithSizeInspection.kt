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
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.callExpressionVisitor

class ReplaceCollectionCountWithSizeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor { callExpression ->
            if (callExpression.isCount()) {
                holder.registerProblem(
                    callExpression,
                    "Replace 'count' with 'size'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceCollectionCountWithSizeQuickFix()
                )
            }
        }
    }
}

class ReplaceCollectionCountWithSizeQuickFix : LocalQuickFix {
    override fun getName() = "Replace 'count' with 'size'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtCallExpression
        element.replace(KtPsiFactory(element).createExpression("size"))
    }
}

private fun KtCallExpression.isCount(): Boolean {
    return isCalling(FqName("kotlin.collections.count")) && (this.valueArguments.size == 0)
}
