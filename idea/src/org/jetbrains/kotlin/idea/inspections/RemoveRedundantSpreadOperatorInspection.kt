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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.isArrayOfMethod
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveRedundantSpreadOperatorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitArgument(argument: KtValueArgument) {
                super.visitArgument(argument)
                argument.getSpreadElement() ?: return
                val argumentExpression = argument.getArgumentExpression() as? KtCallExpression ?: return
                if (argumentExpression.isArrayOfMethod()) {
                    holder.registerProblem(argument,
                                           "Remove redundant spread operator",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           RemoveRedundantSpreadOperatorQuickfix())
                }
            }
        }
    }
}

class RemoveRedundantSpreadOperatorQuickfix : LocalQuickFix {
    override fun getFamilyName() = "Remove redundant spread operator"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val valueArgument = descriptor.psiElement as? KtValueArgument ?: return
        val argumentExpression = valueArgument.getArgumentExpression() as? KtCallExpression ?: return
        val valueArgumentList = argumentExpression.valueArgumentList ?: return
        val factory = KtPsiFactory(project)
        valueArgument.getStrictParentOfType<KtValueArgumentList>()?.let {
            valueArgumentList.arguments.reversed().forEach {
                argument ->
                val newValueArgument = if (argument.isNamed()) {
                    factory.createArgument(argument.getArgumentExpression())
                }
                else argument
                it.addArgumentAfter(newValueArgument, valueArgument)
            }
            it.removeArgument(valueArgument)
        }
    }
}