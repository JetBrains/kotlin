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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class WhenWithOnlyElseInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitWhenExpression(expression: KtWhenExpression) {
                val singleEntry = expression.entries.singleOrNull()
                if (singleEntry?.isElse != true) return

                val usedAsExpression = expression.isUsedAsExpression(expression.analyze())

                holder.registerProblem(expression,
                                       "'when' has only 'else' branch and can be simplified",
                                       SimplifyFix(usedAsExpression)
                )
            }
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