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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext

public open class ChangeVisibilityModifierIntention protected constructor(
        val modifier: KtModifierKeywordToken
) : JetSelfTargetingRangeIntention<KtDeclaration>(javaClass(), "Make ${modifier.getValue()}") {

    override fun applicabilityRange(element: KtDeclaration): TextRange? {
        val modifierList = element.getModifierList()
        if (modifierList?.hasModifier(modifier) ?: false) return null

        var descriptor = element.toDescriptor() as? DeclarationDescriptorWithVisibility ?: return null
        val targetVisibility = modifier.toVisibility()
        if (descriptor.getVisibility() == targetVisibility) return null

        if (modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false) {
            val callableDescriptor = descriptor  as? CallableDescriptor ?: return null
            // cannot make visibility less than (or non-comparable with) any of the supers
            if (callableDescriptor.getOverriddenDescriptors()
                    .map { Visibilities.compare(it.getVisibility(), targetVisibility) }
                    .any { it == null || it > 0  }) return null
        }

        setText(defaultText)

        val modifierElement = element.visibilityModifier()
        if (modifierElement != null) {
            return modifierElement.getTextRange()
        }

        val defaultRange = noModifierYetApplicabilityRange(element) ?: return null

        if (element is KtPrimaryConstructor && defaultRange.isEmpty()) {
            setText("Make primary constructor ${modifier.getValue()}") // otherwise it may be confusing
        }

        return if (modifierList != null)
            TextRange(modifierList.startOffset, defaultRange.getEndOffset()) //TODO: smaller range? now it includes annotations too
        else
            defaultRange
    }

    private fun KtDeclaration.toDescriptor(): DeclarationDescriptor? {
        val bindingContext = analyze()
        // TODO: temporary code
        if (this is KtPrimaryConstructor) {
            return (this.getContainingClassOrObject().resolveToDescriptor() as ClassDescriptor).getUnsubstitutedPrimaryConstructor()
        }

        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
        if (descriptor is ValueParameterDescriptor) {
            return bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor]
        }
        return descriptor
    }

    override fun applyTo(element: KtDeclaration, editor: Editor) {
        element.setVisibility(modifier)
    }

    private fun KtModifierKeywordToken.toVisibility(): Visibility {
        return when (this) {
            KtTokens.PUBLIC_KEYWORD -> Visibilities.PUBLIC
            KtTokens.PRIVATE_KEYWORD -> Visibilities.PRIVATE
            KtTokens.PROTECTED_KEYWORD -> Visibilities.PROTECTED
            KtTokens.INTERNAL_KEYWORD -> Visibilities.INTERNAL
            else -> throw IllegalArgumentException("Unknown visibility modifier:$this")
        }
    }

    private fun noModifierYetApplicabilityRange(declaration: KtDeclaration): TextRange? {
        if (KtPsiUtil.isLocal(declaration)) return null
        return when (declaration) {
            is KtNamedFunction -> declaration.getFunKeyword()?.getTextRange()
            is KtProperty -> declaration.getValOrVarKeyword().getTextRange()
            is KtClass -> declaration.getClassOrInterfaceKeyword()?.getTextRange()
            is KtObjectDeclaration -> declaration.getObjectKeyword().getTextRange()
            is KtPrimaryConstructor -> declaration.getValueParameterList()?.let { TextRange.from(it.startOffset, 0) } //TODO: use constructor keyword if exist
            is KtSecondaryConstructor -> declaration.getConstructorKeyword().getTextRange()
            is KtParameter -> declaration.getValOrVarKeyword()?.getTextRange()
            else -> null
        }
    }

    public class Public : ChangeVisibilityModifierIntention(KtTokens.PUBLIC_KEYWORD), HighPriorityAction

    public class Private : ChangeVisibilityModifierIntention(KtTokens.PRIVATE_KEYWORD), HighPriorityAction {
        override fun applicabilityRange(element: KtDeclaration): TextRange? {
            return if (canBePrivate(element)) super<ChangeVisibilityModifierIntention>.applicabilityRange(element) else null
        }

        private fun canBePrivate(declaration: KtDeclaration): Boolean {
            if (declaration.getModifierList()?.hasModifier(KtTokens.ABSTRACT_KEYWORD) ?: false) return false
            return true
        }
    }

    public class Protected : ChangeVisibilityModifierIntention(KtTokens.PROTECTED_KEYWORD) {
        override fun applicabilityRange(element: KtDeclaration): TextRange? {
            return if (canBeProtected(element)) super.applicabilityRange(element) else null
        }

        private fun canBeProtected(declaration: KtDeclaration): Boolean {
            var parent = declaration.getParent()
            if (parent is KtClassBody) {
                parent = parent.getParent()
            }
            return parent is KtClass
        }
    }

    public class Internal : ChangeVisibilityModifierIntention(KtTokens.INTERNAL_KEYWORD)
}
