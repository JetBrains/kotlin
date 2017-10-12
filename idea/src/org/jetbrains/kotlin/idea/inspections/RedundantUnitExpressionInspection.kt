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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.*

class RedundantUnitExpressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)

                if (KotlinBuiltIns.FQ_NAMES.unit.shortName() != (expression as? KtNameReferenceExpression)?.getReferencedNameAsName()) {
                    return
                }

                val parent = expression.parent
                if (parent !is KtReturnExpression && parent !is KtBlockExpression) return

                holder.registerProblem(expression,
                                       "Redundant 'Unit'",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       RemoveRedundantUnitFix())
            }
        }
    }
}

private class RemoveRedundantUnitFix : LocalQuickFix {
    override fun getName() = "Remove redundant 'Unit'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtReferenceExpression)?.delete()
    }
}
