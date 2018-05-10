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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.Variance

class RemoveModifierFix(
        element: KtModifierListOwner,
        private val modifier: KtModifierKeywordToken,
        private val isRedundant: Boolean
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    private val text = run {
        val modifierText = modifier.value
        when {
            isRedundant ->
                "Remove redundant '$modifierText' modifier"
            modifier === KtTokens.ABSTRACT_KEYWORD || modifier === KtTokens.OPEN_KEYWORD ->
                "Make ${AddModifierFix.getElementName(element)} not $modifierText"
            else ->
                "Remove '$modifierText' modifier"
        }
    }

    override fun getFamilyName() = "Remove modifier"

    override fun getText() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile) =
            (element?.hasModifier(modifier) ?: false)

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        invoke()
    }

    fun invoke() {
        val element = element ?: return
        //TODO: without this copy&replace we get bad formatting on removing last modifier
        val newElement = element.copy() as KtModifierListOwner
        newElement.removeModifier(modifier)
        element.replace(newElement)
    }

    companion object {
        fun createRemoveModifierFromListOwnerFactory(modifier: KtModifierKeywordToken, isRedundant: Boolean = false): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, KtModifierListOwner::class.java) ?: return null
                    return RemoveModifierFix(modifierListOwner, modifier, isRedundant)
                }
            }
        }

        fun createRemoveModifierFactory(isRedundant: Boolean = false): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val psiElement = diagnostic.psiElement
                    val elementType = psiElement.node.elementType as? KtModifierKeywordToken ?: return null
                    val modifierListOwner = psiElement.getStrictParentOfType<KtModifierListOwner>() ?: return null
                    return RemoveModifierFix(modifierListOwner, elementType, isRedundant)
                }
            }
        }

        fun createRemoveProjectionFactory(isRedundant: Boolean): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val projection = diagnostic.psiElement as KtTypeProjection
                    val elementType = projection.projectionToken?.node?.elementType as? KtModifierKeywordToken ?: return null
                    return RemoveModifierFix(projection, elementType, isRedundant)
                }
            }
        }

        fun createRemoveVarianceFactory(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val psiElement = diagnostic.psiElement as KtTypeParameter
                    val modifier = when (psiElement.variance) {
                        Variance.IN_VARIANCE -> KtTokens.IN_KEYWORD
                        Variance.OUT_VARIANCE -> KtTokens.OUT_KEYWORD
                        else -> return null
                    }
                    return RemoveModifierFix(psiElement, modifier, isRedundant = false)
                }
            }
        }

        fun createRemoveSuspendFactory(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val suspendKeyword = diagnostic.psiElement
                    val modifierList = suspendKeyword.parent as KtDeclarationModifierList
                    val type = modifierList.parent as KtTypeReference
                    if (!type.hasModifier(KtTokens.SUSPEND_KEYWORD)) return null
                    return RemoveModifierFix(type, KtTokens.SUSPEND_KEYWORD, isRedundant = false)
                }
            }
        }

        fun createRemoveLateinitFactory(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
                    val keyword = diagnostic.psiElement
                    val modifierList = keyword.parent as? KtDeclarationModifierList ?: return null
                    val property = modifierList.parent as? KtProperty ?: return null
                    if (!property.hasModifier(KtTokens.LATEINIT_KEYWORD)) return null
                    return RemoveModifierFix(property, KtTokens.LATEINIT_KEYWORD, isRedundant = false)
                }
            }
        }
    }
}
