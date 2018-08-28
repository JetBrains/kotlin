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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreatePropertyDelegateAccessorsActionFactory
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

class ChangeVariableMutabilityFix(
    element: KtValVarKeywordOwner,
    private val makeVar: Boolean,
    private val actionText: String? = null
) : KotlinQuickFixAction<KtValVarKeywordOwner>(element) {

    override fun getText() = actionText ?: if (makeVar) "Make variable mutable" else "Make variable immutable"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        val valOrVar = element.valOrVarKeyword?.node?.elementType ?: return false
        return (valOrVar == KtTokens.VAR_KEYWORD) != makeVar
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)
        val newKeyword = if (makeVar) factory.createVarKeyword() else factory.createValKeyword()
        element.valOrVarKeyword!!.replace(newKeyword)
    }

    companion object {
        val VAL_WITH_SETTER_FACTORY: KotlinSingleIntentionActionFactory = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val accessor = diagnostic.psiElement as KtPropertyAccessor
                return ChangeVariableMutabilityFix(accessor.property, true)
            }
        }

        class ReassignmentActionFactory(val factory: DiagnosticFactory1<*, DeclarationDescriptor>) : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val propertyDescriptor = factory.cast(diagnostic).a
                val declaration =
                    DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor) as? KtValVarKeywordOwner ?: return null
                return ChangeVariableMutabilityFix(declaration, true)
            }
        }

        val VAL_REASSIGNMENT_FACTORY = ReassignmentActionFactory(Errors.VAL_REASSIGNMENT)

        val CAPTURED_VAL_INITIALIZATION_FACTORY = ReassignmentActionFactory(Errors.CAPTURED_VAL_INITIALIZATION)

        val CAPTURED_MEMBER_VAL_INITIALIZATION_FACTORY = ReassignmentActionFactory(Errors.CAPTURED_MEMBER_VAL_INITIALIZATION)

        val VAR_OVERRIDDEN_BY_VAL_FACTORY: KotlinSingleIntentionActionFactory = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val element = diagnostic.psiElement
                return when (element) {
                    is KtProperty, is KtParameter -> ChangeVariableMutabilityFix(element as KtValVarKeywordOwner, true)
                    else -> null
                }
            }
        }

        val VAR_ANNOTATION_PARAMETER_FACTORY: KotlinSingleIntentionActionFactory = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val element = diagnostic.psiElement as KtParameter
                return ChangeVariableMutabilityFix(element, false)
            }
        }

        val LATEINIT_VAL_FACTORY = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val lateinitElement = Errors.INAPPLICABLE_LATEINIT_MODIFIER.cast(diagnostic).psiElement
                val property = lateinitElement.getStrictParentOfType<KtProperty>() ?: return null
                if (property.valOrVarKeyword.text != "val") return null
                return ChangeVariableMutabilityFix(property, makeVar = true)
            }
        }

        val CONST_VAL_FACTORY = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val (modifier, element) = Errors.WRONG_MODIFIER_TARGET.cast(diagnostic).run { a to psiElement }
                if (modifier != KtTokens.CONST_KEYWORD) return null
                val property = element.getStrictParentOfType<KtProperty>() ?: return null
                return ChangeVariableMutabilityFix(property, makeVar = false)
            }
        }

        val DELEGATED_PROPERTY_VAL_FACTORY = object : KotlinSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val element = Errors.DELEGATE_SPECIAL_FUNCTION_MISSING.cast(diagnostic).psiElement
                val property = element.getStrictParentOfType<KtProperty>() ?: return null
                val info = CreatePropertyDelegateAccessorsActionFactory.extractFixData(property, diagnostic).singleOrNull() ?: return null
                if (info.name != OperatorNameConventions.SET_VALUE.asString()) return null
                return ChangeVariableMutabilityFix(property, makeVar = false, actionText = "Change to val")
            }
        }
    }
}
