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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public open class ChangeVisibilityModifierIntention protected(
        val modifier: JetModifierKeywordToken
) : JetSelfTargetingRangeIntention<JetDeclaration>(javaClass(), "Make ${modifier.getValue()}") {

    override fun applicabilityRange(element: JetDeclaration): TextRange? {
        val modifierList = element.getModifierList()
        if (modifierList?.hasModifier(modifier) ?: false) return null
        if (modifierList?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false) return null

        setText(defaultText)

        val modifierToChange = modifierToChange(element)
        if (modifierToChange != null) {
            return modifierToChange.getTextRange()
        }

        val defaultRange = canAddVisibilityModifier(element) ?: return null

        val defaultVisibility = if (element is JetPrimaryConstructor) JetTokens.PUBLIC_KEYWORD else JetTokens.INTERNAL_KEYWORD
        if (modifier == defaultVisibility) return null

        if (element is JetPrimaryConstructor && defaultRange.isEmpty()) {
            setText("Make primary constructor ${modifier.getValue()}") // otherwise it may be confusing
        }

        return if (modifierList != null)
            TextRange(modifierList.startOffset, defaultRange.getEndOffset()) //TODO: smaller range? now it includes annotations too
        else
            defaultRange
    }

    override fun applyTo(element: JetDeclaration, editor: Editor) {
        val modifierToChange = modifierToChange(element)
        if (modifierToChange != null) {
            val newModifier = JetPsiFactory(element).createModifierList(modifier).getModifier(modifier)!!
            modifierToChange.replace(newModifier)
        }
        else {
            element.addModifier(modifier)
        }
    }

    private fun modifierToChange(element: JetDeclaration): PsiElement? {
        val modifierList = element.getModifierList() ?: return null
        return JetTokens.VISIBILITY_MODIFIERS.getTypes()
                       .asSequence()
                       .map { modifierList.getModifier(it as JetModifierKeywordToken) }
                       .firstOrNull { it != null } ?: return null
    }

    private fun canAddVisibilityModifier(declaration: JetDeclaration): TextRange? {
        if (JetPsiUtil.isLocal(declaration)) return null
        return when (declaration) {
            is JetNamedFunction -> declaration.getFunKeyword()?.getTextRange()
            is JetProperty -> declaration.getValOrVarNode().getTextRange()
            is JetClass -> declaration.getClassOrTraitKeyword()?.getTextRange()
            is JetObjectDeclaration -> declaration.getObjectKeyword().getTextRange()
            is JetPrimaryConstructor -> declaration.getValueParameterList()?.let { TextRange.from(it.startOffset, 0) } //TODO: use constructor keyword if exist
            is JetSecondaryConstructor -> declaration.getConstructorKeyword().getTextRange()
            is JetParameter -> declaration.getValOrVarNode()?.getTextRange()
            else -> null
        }
    }

    public class Public : ChangeVisibilityModifierIntention(JetTokens.PUBLIC_KEYWORD), HighPriorityAction

    public class Private : ChangeVisibilityModifierIntention(JetTokens.PRIVATE_KEYWORD), HighPriorityAction {
        override fun applicabilityRange(element: JetDeclaration): TextRange? {
            return if (canBePrivate(element)) super<ChangeVisibilityModifierIntention>.applicabilityRange(element) else null
        }

        private fun canBePrivate(declaration: JetDeclaration): Boolean {
            if (declaration.getModifierList()?.hasModifier(JetTokens.ABSTRACT_KEYWORD) ?: false) return false
            return true
        }
    }

    public class Protected : ChangeVisibilityModifierIntention(JetTokens.PROTECTED_KEYWORD) {
        override fun applicabilityRange(element: JetDeclaration): TextRange? {
            return if (canBeProtected(element)) super.applicabilityRange(element) else null
        }

        private fun canBeProtected(declaration: JetDeclaration): Boolean {
            var parent = declaration.getParent()
            if (parent is JetClassBody) {
                parent = parent.getParent()
            }
            return parent is JetClass
        }
    }

    public class Internal : ChangeVisibilityModifierIntention(JetTokens.INTERNAL_KEYWORD)
}
