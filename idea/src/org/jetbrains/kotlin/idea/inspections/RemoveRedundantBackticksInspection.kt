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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.*

class RemoveRedundantBackticksInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                val calleeExpression = expression.calleeExpression ?: return
                if (calleeExpression.text.contains("^`.+`$".toRegex())) {
                    holder.registerProblem(expression,
                                           "Remove redundant backticks",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           RemoveRedundantBackticksQuickFix())
                }
            }
        }
    }
}

class RemoveRedundantBackticksQuickFix : LocalQuickFix {
    override fun getName() = "Remove redundant backticks"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement as? KtCallExpression ?: return
        val calleeExpression = callExpression.calleeExpression ?: return
        val factory = KtPsiFactory(project)
        calleeExpression.replace(factory.createExpression(calleeExpression.text.removePrefix("`").removeSuffix("`")))
    }
}