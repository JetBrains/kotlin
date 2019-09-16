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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.idea.core.canBeInternal
import org.jetbrains.kotlin.idea.core.canBePrivate
import org.jetbrains.kotlin.idea.core.canBeProtected
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.inspections.RemoveRedundantSetterFix
import org.jetbrains.kotlin.idea.inspections.isRedundantSetter
import org.jetbrains.kotlin.idea.util.runOnExpectAndAllActuals
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.ExposedVisibilityChecker

open class ChangeVisibilityFix(
    element: KtModifierListOwner,
    private val elementName: String,
    private val visibilityModifier: KtModifierKeywordToken,
    private val addImplicitVisibilityModifier: Boolean = false
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    override fun getText() = "Make '$elementName' $visibilityModifier"
    override fun getFamilyName() = "Make $visibilityModifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val pointer = element?.createSmartPointer()
        val originalElement = pointer?.element
        if (originalElement is KtDeclaration) {
            originalElement.runOnExpectAndAllActuals(useOnSelf = true) { it.setVisibility(visibilityModifier, addImplicitVisibilityModifier) }
        } else {
            originalElement?.setVisibility(visibilityModifier, addImplicitVisibilityModifier)
        }

        val propertyAccessor = pointer?.element as? KtPropertyAccessor
        if (propertyAccessor?.isRedundantSetter() == true) {
            RemoveRedundantSetterFix.removeRedundantSetter(propertyAccessor)
        }
    }

    protected class ChangeToPublicFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PUBLIC_KEYWORD), HighPriorityAction

    protected class ChangeToProtectedFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PROTECTED_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBeProtected()
        }
    }

    protected class ChangeToInternalFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.INTERNAL_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBeInternal()
        }
    }

    protected class ChangeToPrivateFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PRIVATE_KEYWORD), HighPriorityAction {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBePrivate()
        }
    }

    companion object {
        fun create(
            declaration: KtModifierListOwner,
            descriptor: DeclarationDescriptorWithVisibility,
            targetVisibility: Visibility
        ): IntentionAction? {
            if (!ExposedVisibilityChecker().checkDeclarationWithVisibility(declaration, descriptor, targetVisibility)) return null

            val name = descriptor.name.asString()

            return when (targetVisibility) {
                Visibilities.PRIVATE -> ChangeToPrivateFix(declaration, name)
                Visibilities.INTERNAL -> ChangeToInternalFix(declaration, name)
                Visibilities.PROTECTED -> ChangeToProtectedFix(declaration, name)
                Visibilities.PUBLIC -> ChangeToPublicFix(declaration, name)
                else -> null
            }
        }
    }

    object SetExplicitVisibilityFactory : KotlinIntentionActionsFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val factory = diagnostic.factory as DiagnosticFactory1<*, DeclarationDescriptor>
            val descriptor = factory.cast(diagnostic).a as? DeclarationDescriptorWithVisibility ?: return emptyList()
            val element = diagnostic.psiElement as? KtModifierListOwner ?: return emptyList()
            return listOf(
                ChangeVisibilityFix(
                    element,
                    descriptor.name.asString(),
                    KtTokens.PUBLIC_KEYWORD,
                    addImplicitVisibilityModifier = true
                )
            )
        }
    }
}