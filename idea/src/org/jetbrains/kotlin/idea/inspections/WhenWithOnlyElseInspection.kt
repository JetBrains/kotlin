/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.whenExpressionVisitor
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class WhenWithOnlyElseInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return whenExpressionVisitor { expression ->
            val singleEntry = expression.entries.singleOrNull()
            if (singleEntry?.isElse != true) return@whenExpressionVisitor

            val usedAsExpression = expression.isUsedAsExpression(expression.analyze())

            val subjectVariable = expression.subjectVariable
            val subjectVariableName = subjectVariable?.nameAsName
            if (subjectVariableName != null
                && !usedAsExpression
                && ReferencesSearch.search(subjectVariable, LocalSearchScope(expression)).toList().size > 1
            ) {
                val block = expression.getStrictParentOfType<KtBlockExpression>()
                if (block?.anyDescendantOfType<KtProperty> { it != subjectVariable && it.nameAsName == subjectVariableName } == true) {
                    return@whenExpressionVisitor
                }
            }

            holder.registerProblem(
                expression,
                "'when' has only 'else' branch and should be simplified",
                SimplifyFix(usedAsExpression)
            )
        }
    }

    private class SimplifyFix(
        private val isUsedAsExpression: Boolean
    ) : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Simplify expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val whenExpression = descriptor.psiElement as? KtWhenExpression ?: return
            FileModificationService.getInstance().preparePsiElementForWrite(whenExpression)

            whenExpression.replaceWithBranch(whenExpression.elseExpression ?: return, isUsedAsExpression)
        }
    }
}