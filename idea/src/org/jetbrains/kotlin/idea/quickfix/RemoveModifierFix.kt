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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.psi.JetTypeProjection
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.Variance

public class RemoveModifierFix(
        element: JetModifierListOwner,
        private val modifier: JetModifierKeywordToken,
        private val isRedundant: Boolean
) : KotlinQuickFixAction<JetModifierListOwner>(element) {

    private val text = run {
        val modifierText = modifier.value
        when {
            isRedundant ->
                "Remove redundant '$modifierText' modifier"
            modifier === JetTokens.ABSTRACT_KEYWORD || modifier === JetTokens.OPEN_KEYWORD ->
                "Make ${AddModifierFix.getElementName(element)} not $modifierText"
            else ->
                "Remove '$modifierText' modifier"
        }
    }

    override fun getFamilyName() = "Remove modifier"

    override fun getText() = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile)
            = super.isAvailable(project, editor, file) && element.hasModifier(modifier)

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        invoke()
    }

    public fun invoke() {
        //TODO: without this copy&replace we get bad formatting on removing last modifier
        val newElement = element.copy() as JetModifierListOwner
        newElement.removeModifier(modifier)
        element.replace(newElement)
    }

    companion object {
        public fun createRemoveModifierFromListOwnerFactory(modifier: JetModifierKeywordToken, isRedundant: Boolean = false): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<JetModifierListOwner>? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetModifierListOwner>()) ?: return null
                    return RemoveModifierFix(modifierListOwner, modifier, isRedundant)
                }
            }
        }

        public fun createRemoveModifierFactory(isRedundant: Boolean = false): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<JetModifierListOwner>? {
                    val psiElement = diagnostic.psiElement
                    val elementType = psiElement.node.elementType as? JetModifierKeywordToken ?: return null
                    val modifierListOwner = psiElement.getStrictParentOfType<JetModifierListOwner>() ?: return null
                    return RemoveModifierFix(modifierListOwner, elementType, isRedundant)
                }
            }
        }

        public fun createRemoveProjectionFactory(isRedundant: Boolean): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<JetModifierListOwner>? {
                    val projection = diagnostic.psiElement as JetTypeProjection
                    val elementType = projection.projectionToken?.node?.elementType as? JetModifierKeywordToken ?: return null
                    return RemoveModifierFix(projection, elementType, isRedundant)
                }
            }
        }

        public fun createRemoveVarianceFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<JetModifierListOwner>? {
                    val psiElement = diagnostic.psiElement as JetTypeParameter
                    val modifier = when (psiElement.variance) {
                        Variance.IN_VARIANCE -> JetTokens.IN_KEYWORD
                        Variance.OUT_VARIANCE -> JetTokens.OUT_KEYWORD
                        else -> return null
                    }
                    return RemoveModifierFix(psiElement, modifier, isRedundant = false)
                }
            }
        }
    }
}
