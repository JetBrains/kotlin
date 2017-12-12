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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceToWithInfixFormInspection : AbstractKotlinInspection() {
    private val compatibleNames = setOf("to")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                if (expression.callExpression?.valueArguments?.size != 1) return
                if (expression.calleeName !in compatibleNames) return

                val context = expression.analyze(BodyResolveMode.FULL)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val function = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return

                if (!function.isInfix) return

                holder.registerProblem(
                    expression,
                    "Replace 'to' with infix form",
                    ProblemHighlightType.WEAK_WARNING,
                    ReplaceToWithInfixFormQuickfix()
                )
            }
        }
    }
}

class ReplaceToWithInfixFormQuickfix : LocalQuickFix {
    override fun getName() = "Replace 'to' with infix form"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 to $1", element.receiverExpression,
                                                                        element.callExpression?.valueArguments?.get(0)?.getArgumentExpression() ?: return))
    }
}