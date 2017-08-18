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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinRedundantOverrideInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    super.visitNamedFunction(function)
                    val funKeyword = function.funKeyword ?: return
                    val modifierList = function.modifierList ?: return
                    if (!modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
                    if (MODIFIER_EXCLUDE_OVERRIDE.any { modifierList.hasModifier(it) }) return
                    if (function.annotationEntries.isNotEmpty()) return

                    val bodyExpression = function.bodyExpression ?: return
                    val qualifiedExpression = when (bodyExpression) {
                        is KtDotQualifiedExpression -> bodyExpression
                        is KtBlockExpression -> {
                            val body = bodyExpression.statements.singleOrNull()
                            when (body) {
                                is KtReturnExpression -> body.returnedExpression
                                is KtDotQualifiedExpression -> body.takeIf {
                                    function.typeReference.let { it == null || it.text == "Unit" }
                                }
                                else -> null
                            }

                        }
                        else -> null
                    } as? KtDotQualifiedExpression ?: return

                    val superExpression = qualifiedExpression.receiverExpression as? KtSuperExpression ?: return
                    if (superExpression.superTypeQualifier != null) return

                    val superCallElement = qualifiedExpression.selectorExpression as? KtCallElement ?: return
                    if (!isSameFunctionName(superCallElement, function)) return
                    if (!isSameArguments(superCallElement, function)) return

                    val descriptor = holder.manager.createProblemDescriptor(
                            function,
                            TextRange(modifierList.startOffsetInParent, funKeyword.endOffset - function.startOffset),
                            "Redundant override",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            isOnTheFly,
                            RedundantOverrideFix()
                    )
                    holder.registerProblem(descriptor)
                }
            }

    private fun isSameArguments(superCallElement: KtCallElement, function: KtNamedFunction): Boolean {
        val arguments = superCallElement.valueArguments
        val parameters = function.valueParameters
        if (arguments.size != parameters.size) return false
        return arguments.zip(parameters).all { (argument, parameter) ->
            argument.getArgumentExpression()?.text == parameter.name
        }
    }

    private fun isSameFunctionName(superSelectorExpression: KtCallElement, function: KtNamedFunction): Boolean {
        val superCallMethodName = superSelectorExpression.getCallNameExpression()?.text ?: return false
        return function.name == superCallMethodName
    }

    private class RedundantOverrideFix : LocalQuickFix {
        override fun getName() = "Remove redundant override"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }

    companion object {
        private val MODIFIER_EXCLUDE_OVERRIDE = KtTokens.MODIFIER_KEYWORDS_ARRAY.asList() - KtTokens.OVERRIDE_KEYWORD
    }
}