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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.util.runOnExpectAndAllActuals
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

/** Similar to [AddModifierFix] but with multi-platform support. */
open class AddModifierFixMpp(
    element: KtModifierListOwner,
    modifier: KtModifierKeywordToken
) : AddModifierFix(element, modifier) {

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        val originalElement = element
        if (originalElement is KtDeclaration && modifier.isMultiplatformPersistent()) {
            originalElement.runOnExpectAndAllActuals(useOnSelf = true) { invokeOnElement(it) }
        } else {
            invokeOnElement(originalElement)
        }
    }

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return element.canRefactor()
    }

    companion object : Factory<AddModifierFixMpp> {
        private fun KtModifierKeywordToken.isMultiplatformPersistent(): Boolean =
            this in MODALITY_MODIFIERS || this == INLINE_KEYWORD

        override fun createModifierFix(element: KtModifierListOwner, modifier: KtModifierKeywordToken): AddModifierFixMpp =
            AddModifierFixMpp(element, modifier)
    }

    object MakeClassOpenFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeReference = diagnostic.psiElement as KtTypeReference
            val declaration = typeReference.classForRefactor() ?: return null
            if (declaration.isEnum() || declaration.isData()) return null
            return AddModifierFixMpp(declaration, OPEN_KEYWORD)
        }
    }

    object AddLateinitFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val property = Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT.cast(diagnostic).psiElement
            if (!property.isVar) return null

            val descriptor = property.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return null
            val type = (descriptor as? PropertyDescriptor)?.type ?: return null

            if (TypeUtils.isNullableType(type)) return null
            if (KotlinBuiltIns.isPrimitiveType(type)) return null

            return AddModifierFixMpp(property, LATEINIT_KEYWORD)
        }
    }
}

fun KtTypeReference.classForRefactor(): KtClass? {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    val type = bindingContext[BindingContext.TYPE, this] ?: return null
    val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor) as? KtClass ?: return null
    if (!declaration.canRefactor()) return null
    return declaration
}