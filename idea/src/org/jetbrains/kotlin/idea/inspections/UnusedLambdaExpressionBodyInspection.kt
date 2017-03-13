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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi


class UnusedLambdaExpressionBodyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val context = expression.analyze(BodyResolveMode.PARTIAL)
                if (expression.used(context)) {
                    return
                }

                val descriptor = expression.getResolvedCall(context)?.resultingDescriptor ?: return
                if (!descriptor.returnsFunction()) {
                    return
                }

                val function = descriptor.source.getPsi() as? KtFunction ?: return
                if (function.hasBlockBody() || function.bodyExpression !is KtLambdaExpression) {
                    return
                }

                holder.registerProblem(expression,
                                       "Unused return value of a function with lambda expression body",
                                       RemoveEqTokenFromFunctionDeclarationFix(function))
            }
        }
    }

    private fun KtExpression.used(context: BindingContext): Boolean = context[BindingContext.USED_AS_EXPRESSION, this] ?: true

    private fun CallableDescriptor.returnsFunction() = returnType?.isFunctionType ?: false

    class RemoveEqTokenFromFunctionDeclarationFix(val function: KtFunction) : LocalQuickFix {
        override fun getName(): String = "Remove '=' token from function declaration"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(function)) {
                return
            }

            function.equalsToken?.apply {
                // TODO: This should be done by formatter but there is no rule for this now
                if (prevSibling.isSpace() && nextSibling.isSpace()) {
                    prevSibling.delete()
                }
                delete()
            }
        }

        private fun PsiElement.isSpace() = text == " "
    }
}