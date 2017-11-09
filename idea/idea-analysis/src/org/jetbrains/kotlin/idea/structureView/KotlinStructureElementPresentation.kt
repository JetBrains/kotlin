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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.LocationPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Iconable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.PsiIconUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.renderer.DescriptorRenderer.Companion.ONLY_NAMES_WITH_SHORT_TYPES
import org.jetbrains.kotlin.resolve.DescriptorUtils.getAllOverriddenDeclarations
import org.jetbrains.kotlin.resolve.OverridingUtil.filterOutOverridden
import javax.swing.Icon

internal class KotlinStructureElementPresentation(
        private val isInherited: Boolean,
        navigatablePsiElement: NavigatablePsiElement,
        descriptor: DeclarationDescriptor?
) : ColoredItemPresentation, LocationPresentation {
    private val attributesKey = getElementAttributesKey(isInherited, navigatablePsiElement)
    private val elementText = getElementText(navigatablePsiElement, descriptor)
    private val locationString = getElementLocationString(isInherited, descriptor)
    private val icon = getElementIcon(navigatablePsiElement, descriptor)

    override fun getTextAttributesKey() = attributesKey

    override fun getPresentableText() = elementText

    override fun getLocationString() = locationString

    override fun getIcon(unused: Boolean) = icon

    override fun getLocationPrefix(): String {
        return if (isInherited) " " else LocationPresentation.DEFAULT_LOCATION_PREFIX
    }

    override fun getLocationSuffix(): String {
        return if (isInherited) "" else LocationPresentation.DEFAULT_LOCATION_SUFFIX
    }

    private fun getElementAttributesKey(isInherited: Boolean, navigatablePsiElement: NavigatablePsiElement): TextAttributesKey? {
        if (isInherited) {
            return CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
        }

        if (navigatablePsiElement is KtModifierListOwner && KtPsiUtil.isDeprecated(navigatablePsiElement)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES
        }

        return null
    }

    private fun getElementIcon(navigatablePsiElement: NavigatablePsiElement, descriptor: DeclarationDescriptor?): Icon? {
        if (descriptor != null) {
            return KotlinDescriptorIconProvider.getIcon(descriptor, navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY)
        }

        if (!navigatablePsiElement.isValid) {
            return null
        }

        return PsiIconUtil.getProvidersIcon(navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY)
    }

    private fun getElementText(navigatablePsiElement: NavigatablePsiElement, descriptor: DeclarationDescriptor?): String? {
        if (navigatablePsiElement is KtObjectDeclaration && navigatablePsiElement.isObjectLiteral()) {
            return "object" + (navigatablePsiElement.getSuperTypeList()?.text?.let { " : $it" } ?: "")
        }

        if (descriptor != null) {
            return ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
        }

        val text = navigatablePsiElement.name
        if (!text.isNullOrEmpty()) {
            return text
        }

        if (navigatablePsiElement is KtAnonymousInitializer) {
            return "<class initializer>"
        }

        return null
    }

    private fun getElementLocationString(isInherited: Boolean, descriptor: DeclarationDescriptor?): String? {
        if (!(isInherited && descriptor is CallableMemberDescriptor)) return null

        if (descriptor.kind == CallableMemberDescriptor.Kind.DECLARATION) {
            return withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor.containingDeclaration))
        }

        val overridingDescriptors = filterOutOverridden(getAllOverriddenDeclarations(descriptor))
        // Location can be missing when base in synthesized
        return overridingDescriptors.firstOrNull()?.let {
            withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(it.containingDeclaration))
        }
    }

    private fun withRightArrow(str: String): String {
        val rightArrow = '\u2192'
        return if (UIUtil.getLabelFont().canDisplay(rightArrow)) rightArrow + str else "->" + str
    }
}
