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
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class RedundantLambdaArrowInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                val functionLiteral = lambdaExpression.functionLiteral
                if (functionLiteral.valueParameters.isNotEmpty()) return
                val arrow = functionLiteral.arrow ?: return

                holder.registerProblem(
                        arrow,
                        "Redundant lambda arrow",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        DeleteFix())
            }
        }
    }

    class DeleteFix : LocalQuickFix {
        override fun getFamilyName() = "Remove arrow"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            FileModificationService.getInstance().preparePsiElementForWrite(element)
            element.delete()
        }
    }
}