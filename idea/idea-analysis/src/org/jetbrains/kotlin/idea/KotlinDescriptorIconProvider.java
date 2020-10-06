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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.RowIcon;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.psi.KtElement;

import javax.swing.*;

public final class KotlinDescriptorIconProvider {

    private static final Logger LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider");

    private KotlinDescriptorIconProvider() {
    }

    @Nullable
    public static Icon getIcon(@NotNull DeclarationDescriptor descriptor, @Nullable PsiElement declaration, @Iconable.IconFlags int flags) {
        if (declaration != null && !(declaration instanceof KtElement)) {
            return declaration.getIcon(flags);
        }

        Icon result = getBaseIcon(descriptor);
        if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0) {
            RowIcon rowIcon = new RowIcon(2);
            rowIcon.setIcon(result, 0);
            rowIcon.setIcon(getVisibilityIcon(descriptor), 1);
            result = rowIcon;
        }

        return result;
    }

    private static Icon getVisibilityIcon(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof DeclarationDescriptorWithVisibility) {
            DeclarationDescriptorWithVisibility descriptorWithVisibility = (DeclarationDescriptorWithVisibility) descriptor;
            DescriptorVisibility visibility = descriptorWithVisibility.getVisibility().normalize();
            if (visibility == DescriptorVisibilities.PUBLIC) {
                return PlatformIcons.PUBLIC_ICON;
            }

            if (visibility == DescriptorVisibilities.PROTECTED) {
                return PlatformIcons.PROTECTED_ICON;
            }

            if (DescriptorVisibilities.isPrivate(visibility)) {
                return PlatformIcons.PRIVATE_ICON;
            }

            if (visibility == DescriptorVisibilities.INTERNAL) {
                return PlatformIcons.PACKAGE_LOCAL_ICON;
            }
        }

        return null;
    }

    private static Modality getModalitySafe(@NotNull MemberDescriptor descriptor) {
        try {
            return descriptor.getModality();
        }
        catch (InvalidModuleException ex) {
            return Modality.FINAL;
        }
    }

    private static Icon getBaseIcon(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof PackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) {
            return PlatformIcons.PACKAGE_ICON;
        }
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            if (functionDescriptor.getExtensionReceiverParameter() != null) {
                return Modality.ABSTRACT == getModalitySafe(functionDescriptor)
                       ? KotlinIcons.ABSTRACT_EXTENSION_FUNCTION
                       : KotlinIcons.EXTENSION_FUNCTION;
            }

            if (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                return Modality.ABSTRACT == getModalitySafe(functionDescriptor)
                       ? PlatformIcons.ABSTRACT_METHOD_ICON
                       : PlatformIcons.METHOD_ICON;
            }
            else {
                return KotlinIcons.FUNCTION;
            }
        }
        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            switch (classDescriptor.getKind()) {
                case INTERFACE:
                    return KotlinIcons.INTERFACE;
                case ENUM_CLASS:
                    return KotlinIcons.ENUM;
                case ENUM_ENTRY:
                    return KotlinIcons.ENUM;
                case ANNOTATION_CLASS:
                    return KotlinIcons.ANNOTATION;
                case OBJECT:
                    return KotlinIcons.OBJECT;
                case CLASS:
                    return Modality.ABSTRACT == getModalitySafe(classDescriptor) ?
                           KotlinIcons.ABSTRACT_CLASS :
                           KotlinIcons.CLASS;
                default:
                    LOG.warn("No icon for descriptor: " + descriptor);
                    return null;
            }
        }
        if (descriptor instanceof ValueParameterDescriptor) {
            return KotlinIcons.PARAMETER;
        }

        if (descriptor instanceof LocalVariableDescriptor) {
            return ((VariableDescriptor) descriptor).isVar() ? KotlinIcons.VAR : KotlinIcons.VAL;
        }

        if (descriptor instanceof PropertyDescriptor) {
            return ((VariableDescriptor) descriptor).isVar() ? KotlinIcons.FIELD_VAR : KotlinIcons.FIELD_VAL;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            return PlatformIcons.CLASS_ICON;
        }

        if (descriptor instanceof TypeAliasDescriptor) {
            return KotlinIcons.TYPE_ALIAS;
        }

        LOG.warn("No icon for descriptor: " + descriptor);
        return null;
    }
}
