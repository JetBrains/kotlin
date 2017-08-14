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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class KotlinRedundantOverrideInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    super.visitNamedFunction(function)
                    if (!function.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return

                    val bodyExpression = function.bodyExpression
                    bodyExpression ?: return

                    val body = if (bodyExpression is KtDotQualifiedExpression) {
                        bodyExpression
                    }
                    else {
                        if (bodyExpression.children.size != 1) {
                            return
                        }
                        bodyExpression.children[0]
                    }

                    val qualifiedExpression = if (body is KtReturnExpression) {
                        val returnedExpression = body.returnedExpression
                        returnedExpression ?: return
                        returnedExpression as? KtDotQualifiedExpression ?: return
                    }
                    else {
                        body as? KtDotQualifiedExpression ?: return
                    }

                    if (qualifiedExpression.receiverExpression !is KtSuperExpression) return

                    val superSelectorExpression = qualifiedExpression.selectorExpression
                    val superCallElement = superSelectorExpression as? KtCallElement ?: return

                    if (!isSameFunctionName(superSelectorExpression, function)) return

                    if (!isSameArguments(superCallElement, function)) return

                    holder.registerProblem(function,
                                           "Redundant override",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                           RedundantOverrideFix())


                }
            }

    private fun isSameArguments(superCallElement: KtCallElement, function: KtNamedFunction): Boolean {
        val argumentList = superCallElement.valueArgumentList ?: return false
        val parameterList = function.valueParameterList ?: return false
        val diffArguments = argumentList.arguments.filter { argument ->
            parameterList.parameters.forEach { parameter ->
                if (argument.text == parameter.name) {
                    return@filter false
                }
            }
            return@filter true
        }
        return diffArguments.isEmpty()
    }

    private fun isSameFunctionName(superSelectorExpression: KtExpression?, function: KtNamedFunction): Boolean {
        val superCallMethodName = (superSelectorExpression as KtCallElement).getCallNameExpression()?.text ?: return false
        return function.name == superCallMethodName
    }

    private class RedundantOverrideFix : LocalQuickFix {
        override fun getName() = "Remove redundant overrides"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }
}