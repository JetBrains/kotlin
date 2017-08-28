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

class RedundantGetterInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                super.visitPropertyAccessor(accessor)
                if (accessor.isRedundantGetter()) {
                    holder.registerProblem(accessor,
                                           "Redundant getter",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           RemoveRedundantGetterFix())
                }
            }
        }
    }
}

private fun KtPropertyAccessor.isRedundantGetter(): Boolean {
    if (!isGetter) return false
    if (annotationEntries.isNotEmpty()) return false
    val expression = bodyExpression ?: return true
    if (expression is KtNameReferenceExpression) {
        return expression.isFieldText()
    }
    if (expression is KtBlockExpression) {
        val statement = expression.statements.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val returnExpression = statement as? KtReturnExpression ?: return false
        return returnExpression.returnedExpression?.isFieldText() == true
    }
    return false
}

private fun KtExpression.isFieldText(): Boolean = this.textMatches("field")

private class RemoveRedundantGetterFix : LocalQuickFix {
    override fun getName() = "Remove redundant getter"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val accessor = descriptor.psiElement as? KtPropertyAccessor ?: return
        accessor.delete()
    }
}