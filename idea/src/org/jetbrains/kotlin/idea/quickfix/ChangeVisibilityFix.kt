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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.core.canBePrivate
import org.jetbrains.kotlin.idea.core.canBeProtected
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.ExposedVisibilityChecker

open class ChangeVisibilityFix(
        element: KtModifierListOwner,
        private val elementName: String,
        private val visibilityModifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    override fun getText() = "Make '$elementName' $visibilityModifier"
    override fun getFamilyName() = "Make $visibilityModifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.setVisibility(visibilityModifier)
    }

    protected class ChangeToPublicFix(element: KtModifierListOwner, elementName: String) :
            ChangeVisibilityFix(element, elementName, KtTokens.PUBLIC_KEYWORD), HighPriorityAction

    protected class ChangeToProtectedFix(element: KtModifierListOwner, elementName: String) :
            ChangeVisibilityFix(element, elementName, KtTokens.PROTECTED_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
            val element = element ?: return false
            return super.isAvailable(project, editor, file) && element.canBeProtected()
        }
    }

    protected class ChangeToInternalFix(element: KtModifierListOwner, elementName: String) :
            ChangeVisibilityFix(element, elementName, KtTokens.INTERNAL_KEYWORD)

    protected class ChangeToPrivateFix(element: KtModifierListOwner, elementName: String) :
            ChangeVisibilityFix(element, elementName, KtTokens.PRIVATE_KEYWORD), HighPriorityAction {

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
            val element = element ?: return false
            return super.isAvailable(project, editor, file) && element.canBePrivate()
        }
    }

    companion object {
        fun create(
                declaration: KtModifierListOwner,
                descriptor: DeclarationDescriptorWithVisibility,
                targetVisibility: Visibility
        ) : IntentionAction? {
            if (!ExposedVisibilityChecker().checkDeclarationWithVisibility(declaration, descriptor, targetVisibility)) return null

            val name = descriptor.name.asString()

            return when (targetVisibility) {
                Visibilities.PRIVATE ->   ChangeToPrivateFix(declaration, name)
                Visibilities.INTERNAL ->  ChangeToInternalFix(declaration, name)
                Visibilities.PROTECTED -> ChangeToProtectedFix(declaration, name)
                Visibilities.PUBLIC ->    ChangeToPublicFix(declaration, name)
                else ->      null
            }
        }
    }
}