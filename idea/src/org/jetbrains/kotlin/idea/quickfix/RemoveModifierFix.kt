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

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.Variance

public class RemoveModifierFix(
        element: JetModifierListOwner,
        private val modifier: JetModifierKeywordToken,
        private val isRedundant: Boolean
) : JetIntentionAction<JetModifierListOwner>(element) {

    override fun getFamilyName() = JetBundle.message("remove.modifier.family")

    override fun getText() = makeText(element, modifier, isRedundant)

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        invoke()
    }

    public fun invoke() {
        val newElement = element.copy() as JetModifierListOwner
        element.replace(removeModifier(newElement, modifier))
    }

    companion object {
        private fun makeText(element: JetModifierListOwner?, modifier: JetKeywordToken, isRedundant: Boolean): String {
            if (isRedundant) {
                return JetBundle.message("remove.redundant.modifier", modifier.value)
            }
            if (element != null && (modifier === JetTokens.ABSTRACT_KEYWORD || modifier === JetTokens.OPEN_KEYWORD)) {
                return JetBundle.message("make.element.not.modifier", AddModifierFix.getElementName(element), modifier.value)
            }
            return JetBundle.message("remove.modifier", modifier.value)
        }

        private fun <T : JetModifierListOwner> removeModifier(element: T, modifier: JetModifierKeywordToken): T {
            val modifierList = element.modifierList!!
            removeModifierFromList(modifierList, modifier)
            if (modifierList.firstChild == null) {
                val whiteSpace = modifierList.nextSibling
                assert(element is ASTDelegatePsiElement)
                (element as ASTDelegatePsiElement).deleteChildInternal(modifierList.node)
                QuickFixUtil.removePossiblyWhiteSpace(element, whiteSpace)
            }
            return element
        }

        private fun removeModifierFromList(modifierList: JetModifierList, modifier: JetModifierKeywordToken): JetModifierList {
            val modifierNode = modifierList.getModifierNode(modifier)!!
            val whiteSpace = modifierNode.psi.nextSibling
            val wsRemoved = QuickFixUtil.removePossiblyWhiteSpace(modifierList, whiteSpace)
            modifierList.deleteChildInternal(modifierNode)
            if (!wsRemoved) {
                QuickFixUtil.removePossiblyWhiteSpace(modifierList, modifierList.lastChild)
            }
            return modifierList
        }

        public fun createRemoveModifierFromListOwnerFactory(modifier: JetModifierKeywordToken, isRedundant: Boolean = false): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetModifierListOwner>? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetModifierListOwner>()) ?: return null
                    return RemoveModifierFix(modifierListOwner, modifier, isRedundant)
                }
            }
        }

        public fun createRemoveModifierFactory(isRedundant: Boolean = false): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetModifierListOwner>? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetModifierListOwner>()) ?: return null
                    val psiElement = diagnostic.psiElement
                    val elementType = psiElement.node.elementType
                    if (elementType !is JetModifierKeywordToken) return null
                    return RemoveModifierFix(modifierListOwner, elementType, isRedundant)
                }
            }
        }

        public fun createRemoveProjectionFactory(isRedundant: Boolean): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetModifierListOwner>? {
                    val projection = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetTypeProjection>()) ?: return null
                    val projectionAstNode = projection.projectionNode ?: return null
                    val elementType = projectionAstNode.elementType
                    if (elementType !is JetModifierKeywordToken) return null
                    return RemoveModifierFix(projection, elementType, isRedundant)
                }
            }
        }

        public fun createRemoveVarianceFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetModifierListOwner>? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetModifierListOwner>()) ?: return null
                    val psiElement = diagnostic.psiElement
                    if (psiElement !is JetTypeParameter) return null
                    val variance = psiElement.variance
                    val modifier: JetModifierKeywordToken
                    when (variance) {
                        Variance.IN_VARIANCE -> modifier = JetTokens.IN_KEYWORD
                        Variance.OUT_VARIANCE -> modifier = JetTokens.OUT_KEYWORD
                        else -> return null
                    }
                    return RemoveModifierFix(modifierListOwner, modifier, false)
                }
            }
        }
    }
}
