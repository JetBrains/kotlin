/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.EffectiveVisibility.Permissiveness.LESS
import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.canBeProtected
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ExposedVisibilityChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

open class IncreaseVisibilityFix(
        element: KtModifierListOwner,
        private val elementName: String,
        private val visibilityModifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtModifierListOwner>(element), CleanupFix {

    override fun getText() = "Make $elementName $visibilityModifier"
    override fun getFamilyName() = "Make $visibilityModifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element.setVisibility(visibilityModifier)
    }

    class IncreaseToPublicFix(element: KtModifierListOwner, elementName: String) :
            IncreaseVisibilityFix(element, elementName, PUBLIC_KEYWORD), HighPriorityAction

    class IncreaseToProtectedFix(element: KtModifierListOwner, elementName: String) :
            IncreaseVisibilityFix(element, elementName, PROTECTED_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile) =
                super.isAvailable(project, editor, file) && element.canBeProtected()
    }

    class IncreaseToInternalFix(element: KtModifierListOwner, elementName: String) :
            IncreaseVisibilityFix(element, elementName, INTERNAL_KEYWORD)

    companion object : KotlinSingleIntentionActionFactory() {

        private fun create(
                declaration: KtModifierListOwner,
                descriptor: DeclarationDescriptorWithVisibility,
                targetVisibility: Visibility
        ) : IntentionAction? {
            if (!ExposedVisibilityChecker().checkDeclarationWithVisibility(declaration, descriptor, targetVisibility)) return null

            val name = descriptor.name.asString()

            return when (targetVisibility) {
                INTERNAL ->  IncreaseToInternalFix(declaration, name)
                PROTECTED -> IncreaseToProtectedFix(declaration, name)
                PUBLIC ->    IncreaseToPublicFix(declaration, name)
                else ->      null
            }
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement as? KtElement ?: return null
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val usageModule = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, element.getContainingKtFile())?.module

            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory3<*, DeclarationDescriptor, *, DeclarationDescriptor>
            val descriptor = factory.cast(diagnostic).c as? DeclarationDescriptorWithVisibility ?: return null
            val declaration = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtModifierListOwner ?: return null

            val module = DescriptorUtils.getContainingModule(descriptor)
            val targetVisibility = if (module != usageModule || descriptor.visibility != PRIVATE) PUBLIC else INTERNAL

            return create(declaration, descriptor, targetVisibility)
        }
    }

    object Exposed : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory3<*, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>
            val exposedDiagnostic = factory.cast(diagnostic)
            val exposedDescriptor = exposedDiagnostic.b.descriptor as? DeclarationDescriptorWithVisibility ?: return null
            val exposedDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(exposedDescriptor) as? KtModifierListOwner ?: return null
            val exposedVisibility = exposedDiagnostic.c
            val exposingVisibility = exposedDiagnostic.a
            val boundVisibility = when (exposedVisibility.relation(exposingVisibility)) {
                LESS -> exposingVisibility.toVisibility()
                else -> PUBLIC
            }
            val exposingDeclaration = diagnostic.psiElement.getParentOfType<KtDeclaration>(true)
            val targetVisibility = when (boundVisibility) {
                PROTECTED -> if (exposedDeclaration.parent == exposingDeclaration?.parent) PROTECTED else PUBLIC
                else -> boundVisibility
            }
            return create(exposedDeclaration, exposedDescriptor, targetVisibility)
        }
    }
}
