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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class ChangeVariableMutabilityFix(element: KtValVarKeywordOwner, private val makeVar: Boolean) : KotlinQuickFixAction<KtValVarKeywordOwner>(element) {

    override fun getText() = if (makeVar) "Make variable mutable" else "Make variable immutable"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val valOrVar = element.valOrVarKeyword?.node?.elementType ?: return false
        return (valOrVar == KtTokens.VAR_KEYWORD) != makeVar
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val factory = KtPsiFactory(project)
        val newKeyword = if (makeVar) factory.createVarKeyword() else factory.createValKeyword()
        element.valOrVarKeyword!!.replace(newKeyword)
    }

    companion object {
        val VAL_WITH_SETTER_FACTORY: KotlinSingleIntentionActionFactory = object: KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val accessor = diagnostic.psiElement as KtPropertyAccessor
                return ChangeVariableMutabilityFix(accessor.property, true)
            }
        }

        val VAL_REASSIGNMENT_FACTORY: KotlinSingleIntentionActionFactory = object: KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val propertyDescriptor = Errors.VAL_REASSIGNMENT.cast(diagnostic).a
                val declaration = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor) as? KtValVarKeywordOwner ?: return null
                return ChangeVariableMutabilityFix(declaration, true)
            }
        }

        val VAR_OVERRIDDEN_BY_VAL_FACTORY: KotlinSingleIntentionActionFactory = object: KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val element = diagnostic.psiElement
                return when (element) {
                    is KtProperty, is KtParameter -> ChangeVariableMutabilityFix(element as KtValVarKeywordOwner, true)
                    else -> null
                }
            }
        }
    }
}
