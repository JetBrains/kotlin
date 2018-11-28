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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UnnecessaryVariableInspection : AbstractApplicabilityBasedInspection<KtProperty>(KtProperty::class.java) {

    override fun isApplicable(element: KtProperty) = statusFor(element) != null

    override fun inspectionTarget(element: KtProperty) = element.nameIdentifier ?: element

    override fun inspectionText(element: KtProperty) = when (statusFor(element)) {
        Status.RETURN_ONLY ->
            "Variable used only in following return and should be inlined"
        Status.EXACT_COPY ->
            "Variable is same as '${(element.initializer as? KtNameReferenceExpression)?.getReferencedName()}' and should be inlined"
        else -> ""
    }

    override val defaultFixText = "Inline variable"

    override val startFixInWriteAction = false

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val property = element.getParentOfType<KtProperty>(strict = false) ?: return
        KotlinInlineValHandler(withPrompt = false).inlineElement(project, editor, property)
    }

    companion object {
        private enum class Status {
            RETURN_ONLY,
            EXACT_COPY
        }

        private fun statusFor(property: KtProperty): Status? {
            val enclosingElement = KtPsiUtil.getEnclosingElementForLocalDeclaration(property) ?: return null
            val initializer = property.initializer ?: return null

            fun isExactCopy(): Boolean {
                if (!property.isVar && initializer is KtNameReferenceExpression && property.typeReference == null) {
                    val initializerDescriptor = initializer.resolveToCall(BodyResolveMode.FULL)?.resultingDescriptor as? VariableDescriptor
                            ?: return false
                    if (initializerDescriptor.isVar) return false
                    if (initializerDescriptor.containingDeclaration !is FunctionDescriptor) return false

                    val copyName = initializerDescriptor.name.asString()
                    if (ReferencesSearch.search(property, LocalSearchScope(enclosingElement)).findFirst() == null) return false

                    val containingDeclaration = property.getStrictParentOfType<KtDeclaration>()
                    if (containingDeclaration != null) {
                        val validator = NewDeclarationNameValidator(
                            container = containingDeclaration,
                            anchor = property,
                            target = NewDeclarationNameValidator.Target.VARIABLES,
                            excludedDeclarations = listOfNotNull(
                                DescriptorToSourceUtils.descriptorToDeclaration(initializerDescriptor) as? KtDeclaration
                            )
                        )
                        if (!validator(copyName)) return false
                    }
                    return true
                }
                return false
            }

            fun isReturnOnly(): Boolean {
                val nextStatement = property.getNextSiblingIgnoringWhitespaceAndComments() as? KtReturnExpression ?: return false
                val returned = nextStatement.returnedExpression as? KtNameReferenceExpression ?: return false
                val context = nextStatement.analyze()
                return context[REFERENCE_TARGET, returned] == context[DECLARATION_TO_DESCRIPTOR, property]
            }

            return when {
                isExactCopy() -> Status.EXACT_COPY
                isReturnOnly() -> Status.RETURN_ONLY
                else -> null
            }
        }
    }
}