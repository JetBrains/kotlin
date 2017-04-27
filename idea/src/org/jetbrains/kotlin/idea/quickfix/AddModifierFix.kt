/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

open class AddModifierFix(
        element: KtModifierListOwner,
        protected val modifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    override fun getText(): String {
        val element = element ?: return ""
        if (modifier in modalityModifiers || modifier in KtTokens.VISIBILITY_MODIFIERS) {
            return "Make ${getElementName(element)} ${modifier.value}"
        }
        return "Add '${modifier.value}' modifier"
    }

    override fun getFamilyName() = "Add modifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addModifier(modifier)

        if (modifier == KtTokens.ABSTRACT_KEYWORD && (element is KtProperty || element is KtNamedFunction)) {
            element?.containingClass()?.run {
                if (!hasModifier(KtTokens.ABSTRACT_KEYWORD) && !hasModifier(KtTokens.SEALED_KEYWORD)) {
                    addModifier(KtTokens.ABSTRACT_KEYWORD)
                }
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && element.canRefactor()
    }

    companion object {
        private val modalityModifiers = setOf(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD)

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

        fun createFactory(modifier: KtModifierKeywordToken): KotlinSingleIntentionActionFactory {
            return createFactory(modifier, KtModifierListOwner::class.java)
        }

        fun <T : KtModifierListOwner> createFactory(modifier: KtModifierKeywordToken, modifierOwnerClass: Class<T>): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, modifierOwnerClass) ?: return null
                    return createIfApplicable(modifierListOwner, modifier)
                }
            }
        }

        fun createIfApplicable(modifierListOwner: KtModifierListOwner, modifier: KtModifierKeywordToken): AddModifierFix? {
            if (modifier == ABSTRACT_KEYWORD || modifier == OPEN_KEYWORD) {
                if (modifierListOwner is KtObjectDeclaration) return null
                if (modifierListOwner is KtEnumEntry) return null
                if (modifierListOwner is KtDeclaration && modifierListOwner !is KtClass) {
                    val parentClassOrObject = modifierListOwner.containingClassOrObject ?: return null
                    if (parentClassOrObject is KtObjectDeclaration) return null
                    if (parentClassOrObject is KtEnumEntry) return null
                }
            }
            return AddModifierFix(modifierListOwner, modifier)
        }

    }

    object MakeClassOpenFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeReference = diagnostic.psiElement as KtTypeReference
            val bindingContext = typeReference.analyze(BodyResolveMode.PARTIAL)
            val type = bindingContext[BindingContext.TYPE, typeReference] ?: return null
            val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor) as? KtClass ?: return null
            if (!declaration.canRefactor()) return null
            if (declaration.isEnum()) return null
            return AddModifierFix(declaration, KtTokens.OPEN_KEYWORD)
        }
    }

    object AddLateinitFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val property = Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT.cast(diagnostic).psiElement
            if (!property.isVar) return null

            val context = property.analyze()
            val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] ?: return null
            val type = (descriptor as? PropertyDescriptor)?.type ?: return null

            if (TypeUtils.isNullableType(type)) return null
            if (KotlinBuiltIns.isPrimitiveType(type)) return null

            return AddModifierFix(property, KtTokens.LATEINIT_KEYWORD)
        }
    }
}
