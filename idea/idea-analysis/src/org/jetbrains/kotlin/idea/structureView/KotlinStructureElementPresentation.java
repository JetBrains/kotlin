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

package org.jetbrains.kotlin.idea.structureView;

import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.LocationPresentation;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.PsiIconUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider;
import org.jetbrains.kotlin.psi.KtClassInitializer;
import org.jetbrains.kotlin.psi.KtModifierListOwner;
import org.jetbrains.kotlin.psi.KtPsiUtil;

import javax.swing.*;
import java.util.Set;

import static org.jetbrains.kotlin.renderer.DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getAllOverriddenDeclarations;
import static org.jetbrains.kotlin.resolve.OverrideResolver.filterOutOverridden;

class KotlinStructureElementPresentation implements ColoredItemPresentation, LocationPresentation {
    private final TextAttributesKey attributesKey;
    private final String elementText;
    private final String locationString;
    private final Icon icon;
    private final boolean isInherited;

    public KotlinStructureElementPresentation(
            boolean isInherited,
            @NotNull NavigatablePsiElement navigatablePsiElement,
            @Nullable DeclarationDescriptor descriptor
    ) {
        this.isInherited = isInherited;
        attributesKey = getElementAttributesKey(isInherited, navigatablePsiElement);
        elementText = getElementText(navigatablePsiElement, descriptor);
        locationString = getElementLocationString(isInherited, descriptor);
        icon = getElementIcon(navigatablePsiElement, descriptor);
    }

    @Nullable
    @Override
    public TextAttributesKey getTextAttributesKey() {
        return attributesKey;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        return elementText;
    }

    @Nullable
    @Override
    public String getLocationString() {
        return locationString;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public String getLocationPrefix() {
        return isInherited ? " " : LocationPresentation.DEFAULT_LOCATION_PREFIX;
    }

    @Override
    public String getLocationSuffix() {
        return isInherited ? "" : LocationPresentation.DEFAULT_LOCATION_SUFFIX;
    }

    @Nullable
    private static TextAttributesKey getElementAttributesKey(boolean isInherited, @NotNull NavigatablePsiElement navigatablePsiElement) {
        if (isInherited) {
            return CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES;
        }

        if (navigatablePsiElement instanceof KtModifierListOwner && KtPsiUtil.isDeprecated((KtModifierListOwner) navigatablePsiElement)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES;
        }

        return null;
    }

    @Nullable
    private static Icon getElementIcon(@NotNull NavigatablePsiElement navigatablePsiElement, @Nullable DeclarationDescriptor descriptor) {
        if (descriptor != null) {
            return KotlinDescriptorIconProvider.getIcon(descriptor, navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY);
        }

        return PsiIconUtil.getProvidersIcon(navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY);
    }

    @Nullable
    private static String getElementText(@NotNull NavigatablePsiElement navigatablePsiElement, @Nullable DeclarationDescriptor descriptor) {
        if (descriptor != null) {
            return ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor);
        }

        String text = navigatablePsiElement.getName();
        if (!StringUtil.isEmpty(text)) {
            return text;
        }

        if (navigatablePsiElement instanceof KtClassInitializer) {
            return "<class initializer>";
        }

        return null;
    }

    @Nullable
    private static String getElementLocationString(boolean isInherited, @Nullable DeclarationDescriptor descriptor) {
        if (!(isInherited && descriptor instanceof CallableMemberDescriptor)) return null;

        CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;

        if (callableMemberDescriptor.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            return withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(callableMemberDescriptor.getContainingDeclaration()));
        }

        Set<CallableMemberDescriptor> overridingDescriptors = filterOutOverridden(getAllOverriddenDeclarations(callableMemberDescriptor));
        CallableMemberDescriptor firstOverriding = ContainerUtil.getFirstItem(overridingDescriptors);
        if (firstOverriding != null) {
            return withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(firstOverriding.getContainingDeclaration()));
        }

        // Location can be missing when base in synthesized
        return null;
    }

    private static String withRightArrow(String str) {
        char rightArrow = '\u2192';
        return UIUtil.getLabelFont().canDisplay(rightArrow) ? rightArrow + str :  "->" + str;
    }
}
