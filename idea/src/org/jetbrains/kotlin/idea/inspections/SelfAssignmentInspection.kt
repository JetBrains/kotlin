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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class SelfAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)

                val left = expression.left ?: return
                val right = expression.right ?: return

                val context = expression.analyze()

                val leftCallee = left.getResolvedCall(context)?.resultingDescriptor ?: return
                val rightCallee = right.getResolvedCall(context)?.resultingDescriptor ?: return
                if (leftCallee != rightCallee) return

                if (rightCallee is PropertyDescriptor && rightCallee.accessors.any { !it.isDefault }) return

                holder.registerProblem(right,
                                       "Variable '${left.text}' is assigned to itself",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       RemoveSelfAssignmentFix())
            }
        }
    }
}

private class RemoveSelfAssignmentFix : LocalQuickFix {
    override fun getName() = "Remove self assignment"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val right = descriptor.psiElement as? KtExpression ?: return
        right.parent.delete()
    }
}
