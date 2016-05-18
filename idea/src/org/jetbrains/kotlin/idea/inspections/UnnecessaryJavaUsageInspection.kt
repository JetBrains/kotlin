/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

class UnnecessaryJavaUsageInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    val patterns = mapOf(
            "System.out.println($0)" to "println($0)",
            "System.out.print($0)" to "print($0)",
            "Collections.sort($0)" to "$0.sort()"
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                super.visitQualifiedExpression(expression)

                val selectorExpression = expression.selectorExpression ?: return
                if (selectorExpression !is KtCallExpression) return
                if (selectorExpression.valueArguments.size != 1) return
                val value = selectorExpression.valueArguments[0].text
                val pattern = expression.text.replace(value, "$0")
                if (!patterns.containsKey(pattern)) return

                holder.registerProblem(expression,
                                       "Unnecessary java usage",
                                       ProblemHighlightType.WEAK_WARNING,
                                       UnnecessaryJavaUsageFix(patterns[pattern]!!, value))
            }
        }
    }

    private class UnnecessaryJavaUsageFix(val pattern: String, val value: String) : LocalQuickFix {
        override fun getName() = "Unnecessary java usage"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            element.replace(KtPsiFactory(element).createExpressionByPattern(pattern, value));
        }
    }

}


