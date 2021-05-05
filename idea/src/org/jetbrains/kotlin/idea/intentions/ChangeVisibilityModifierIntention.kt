/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.util.runCommandOnAllExpectAndActualDeclaration
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.toVisibility
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

open class ChangeVisibilityModifierIntention protected constructor(val modifier: KtModifierKeywordToken) :
    SelfTargetingRangeIntention<KtDeclaration>(KtDeclaration::class.java, KotlinBundle.lazyMessage("make.0", modifier.value)) {
    override fun startInWriteAction(): Boolean = false

    override fun applicabilityRange(element: KtDeclaration): TextRange? {
        val modifierList = element.modifierList
        if (modifierList?.hasModifier(modifier) == true) return null

        if (KtPsiUtil.isLocal((element as? KtPropertyAccessor)?.property ?: element)) return null

        val descriptor = element.toDescriptor() as? DeclarationDescriptorWithVisibility ?: return null
        val targetVisibility = modifier.toVisibility()
        if (descriptor.visibility == targetVisibility) return null

        if (modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) {
            val callableDescriptor = descriptor as? CallableDescriptor ?: return null
            // cannot make visibility less than (or non-comparable with) any of the supers
            if (callableDescriptor.overriddenDescriptors
                    .map { DescriptorVisibilities.compare(it.visibility, targetVisibility) }
                    .any { it == null || it > 0 }
            ) return null
        }

        setTextGetter(defaultTextGetter)

        if (element is KtPropertyAccessor) {
            if (element.isGetter) return null
            if (targetVisibility == DescriptorVisibilities.PUBLIC) {
                val explicitVisibility = element.modifierList?.visibilityModifierType()?.value ?: return null
                setTextGetter(KotlinBundle.lazyMessage("remove.0.modifier", explicitVisibility))
            } else {
                val propVisibility = (element.property.toDescriptor() as? DeclarationDescriptorWithVisibility)?.visibility ?: return null
                if (propVisibility == targetVisibility) return null
                val compare = DescriptorVisibilities.compare(targetVisibility, propVisibility)
                if (compare == null || compare > 0) return null
            }
        }
        val defaultRange = noModifierYetApplicabilityRange(element) ?: return null

        if (element is KtPrimaryConstructor && defaultRange.isEmpty && element.visibilityModifier() == null) {
            setTextGetter(KotlinBundle.lazyMessage("make.primary.constructor.0", modifier.value)) // otherwise it may be confusing
        }

        return if (modifierList != null)
            TextRange(modifierList.startOffset, defaultRange.endOffset) //TODO: smaller range? now it includes annotations too
        else
            defaultRange
    }

    override fun applyTo(element: KtDeclaration, editor: Editor?) {
        val factory = KtPsiFactory(element)
        element.runCommandOnAllExpectAndActualDeclaration(KotlinBundle.message("change.visibility.modifier"), writeAction = true) {
            it.setVisibility(modifier)
            if (it is KtPropertyAccessor) {
                it.modifierList?.nextSibling?.replace(factory.createWhiteSpace())
            }
        }
    }

    private fun noModifierYetApplicabilityRange(declaration: KtDeclaration): TextRange? = when (declaration) {
        is KtNamedFunction -> declaration.funKeyword?.textRange
        is KtProperty -> declaration.valOrVarKeyword.textRange
        is KtPropertyAccessor -> declaration.namePlaceholder.textRange
        is KtClass -> declaration.getClassOrInterfaceKeyword()?.textRange
        is KtObjectDeclaration -> declaration.getObjectKeyword()?.textRange
        is KtPrimaryConstructor -> declaration.valueParameterList?.let {
            //TODO: use constructor keyword if exist
            TextRange.from(it.startOffset, 0)
        }
        is KtSecondaryConstructor -> declaration.getConstructorKeyword().textRange
        is KtParameter -> declaration.valOrVarKeyword?.textRange
        is KtTypeAlias -> declaration.getTypeAliasKeyword()?.textRange
        else -> null
    }

    class Public : ChangeVisibilityModifierIntention(KtTokens.PUBLIC_KEYWORD), HighPriorityAction {
        override fun applicabilityRange(element: KtDeclaration): TextRange? =
            if (element.canBePublic()) super.applicabilityRange(element) else null
    }

    class Private : ChangeVisibilityModifierIntention(KtTokens.PRIVATE_KEYWORD), HighPriorityAction {
        override fun applicabilityRange(element: KtDeclaration): TextRange? =
            if (element.canBePrivate()) super.applicabilityRange(element) else null
    }

    class Protected : ChangeVisibilityModifierIntention(KtTokens.PROTECTED_KEYWORD) {
        override fun applicabilityRange(element: KtDeclaration): TextRange? =
            if (element.canBeProtected()) super.applicabilityRange(element) else null
    }

    class Internal : ChangeVisibilityModifierIntention(KtTokens.INTERNAL_KEYWORD) {
        override fun applicabilityRange(element: KtDeclaration): TextRange? =
            if (element.canBeInternal()) super.applicabilityRange(element) else null
    }
}
