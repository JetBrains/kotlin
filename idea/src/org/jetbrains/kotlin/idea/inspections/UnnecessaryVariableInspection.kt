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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET

class UnnecessaryVariableInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
            object : KtVisitorVoid() {
                override fun visitProperty(property: KtProperty) {
                    super.visitProperty(property)

                    if (!property.isLocal) return
                    val initializer = property.initializer ?: return
                    val nameIdentifier = property.nameIdentifier ?: return

                    if (!property.isVar && initializer is KtNameReferenceExpression && property.typeReference == null) {
                        val context = property.analyze()
                        val initializerDescriptor = context[REFERENCE_TARGET, initializer]
                        if (initializerDescriptor is VariableDescriptor) {
                            if (!initializerDescriptor.isVar &&
                                initializerDescriptor.containingDeclaration is FunctionDescriptor) {
                                holder.registerProblem(
                                        nameIdentifier,
                                        "Variable is an exact copy of another variable and can be inlined",
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                        InlineVariableFix()
                                )
                                return
                            }
                        }
                    }

                    val nextStatement = property.getNextSiblingIgnoringWhitespaceAndComments()
                    if (nextStatement is KtReturnExpression) {
                        val returned = nextStatement.returnedExpression
                        if (returned is KtNameReferenceExpression) {
                            val context = nextStatement.analyze()
                            if (context[REFERENCE_TARGET, returned] == context[DECLARATION_TO_DESCRIPTOR, property]) {
                                holder.registerProblem(
                                        nameIdentifier,
                                        "Variable used only in following return and can be inlined",
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                        InlineVariableFix()
                                )
                            }
                        }
                    }
                }
            }

    class InlineVariableFix : LocalQuickFix {

        override fun getName() = "Inline variable"

        override fun getFamilyName() = name

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val property = descriptor.psiElement.getParentOfType<KtProperty>(strict = true) ?: return
            KotlinInlineValHandler().inlineElement(project, property.findExistingEditor(), property)
        }
    }
}