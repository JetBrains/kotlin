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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor

open class AddModifierFix(
        element: KtModifierListOwner,
        private val modifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    override fun getText(): String {
        if (modifier == ABSTRACT_KEYWORD || modifier == KtTokens.OPEN_KEYWORD) {
            return JetBundle.message("make.element.modifier", getElementName(element), modifier.value)
        }
        return JetBundle.message("add.modifier", modifier.value)
    }

    override fun getFamilyName() = JetBundle.message("add.modifier.family")

    public override operator fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element.addModifier(modifier)
    }

    override fun startInWriteAction() = true

    companion object {
        fun getElementName(modifierListOwner: KtModifierListOwner): String {
            var name: String? = null
            if (modifierListOwner is PsiNameIdentifierOwner) {
                val nameIdentifier = modifierListOwner.nameIdentifier
                if (nameIdentifier != null) {
                    name = nameIdentifier.text
                }
            }
            else if (modifierListOwner is KtPropertyAccessor) {
                name = modifierListOwner.namePlaceholder.text
            }
            if (name == null) {
                name = modifierListOwner.text
            }
            return "'$name'"
        }

        fun createFactory(modifier: KtModifierKeywordToken): JetSingleIntentionActionFactory {
            return createFactory(modifier, KtModifierListOwner::class.java)
        }

        fun <T : KtModifierListOwner> createFactory(modifier: KtModifierKeywordToken, modifierOwnerClass: Class<T>): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, modifierOwnerClass) ?: return null

                    if (modifier == KtTokens.ABSTRACT_KEYWORD && modifierListOwner is KtObjectDeclaration) return null

                    return AddModifierFix(modifierListOwner, modifier)
                }
            }
        }
    }
}
