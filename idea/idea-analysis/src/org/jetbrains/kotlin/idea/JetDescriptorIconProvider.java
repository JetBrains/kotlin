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
import org.jetbrains.kotlin.psi.JetElement;

import javax.swing.*;

public final class JetDescriptorIconProvider {

    private static final Logger LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.JetDescriptorIconProvider");

    private JetDescriptorIconProvider() {
    }

    @NotNull
    public static Icon getIcon(@NotNull DeclarationDescriptor descriptor, @Nullable PsiElement declaration, @Iconable.IconFlags int flags) {
        if (declaration != null && !(declaration instanceof JetElement)) {
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
            Visibility visibility = descriptorWithVisibility.getVisibility();
            if (visibility == Visibilities.PUBLIC) {
                return PlatformIcons.PUBLIC_ICON;
            }

            if (visibility == Visibilities.PROTECTED) {
                return PlatformIcons.PROTECTED_ICON;
            }

            if (Visibilities.isPrivate(visibility)) {
                return PlatformIcons.PRIVATE_ICON;
            }

            if (visibility == Visibilities.INTERNAL) {
                return PlatformIcons.PACKAGE_LOCAL_ICON;
            }
        }

        return null;
    }

    private static Icon getBaseIcon(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof PackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) {
            return PlatformIcons.PACKAGE_ICON;
        }
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            if (functionDescriptor.getExtensionReceiverParameter() != null) {
                return JetIcons.EXTENSION_FUNCTION;
            }

            if (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                if (Modality.ABSTRACT == functionDescriptor.getModality()) {
                    return PlatformIcons.ABSTRACT_METHOD_ICON;
                }
                else {
                    return PlatformIcons.METHOD_ICON;
                }
            }
            else {
                return JetIcons.FUNCTION;
            }
        }
        if (descriptor instanceof ClassDescriptor) {
            switch (((ClassDescriptor) descriptor).getKind()) {
                case TRAIT:
                    return JetIcons.TRAIT;
                case ENUM_CLASS:
                    return JetIcons.ENUM;
                case ENUM_ENTRY:
                    return JetIcons.ENUM;
                case ANNOTATION_CLASS:
                    return PlatformIcons.ANNOTATION_TYPE_ICON;
                case OBJECT:
                    return JetIcons.OBJECT;
                case CLASS:
                    return JetIcons.CLASS;
                default:
                    LOG.warn("No icon for descriptor: " + descriptor);
                    return null;
            }
        }
        if (descriptor instanceof ValueParameterDescriptor) {
            return JetIcons.PARAMETER;
        }

        if (descriptor instanceof LocalVariableDescriptor) {
            return ((VariableDescriptor) descriptor).isVar() ? JetIcons.VAR : JetIcons.VAL;
        }

        if (descriptor instanceof PropertyDescriptor) {
            return ((VariableDescriptor) descriptor).isVar() ? JetIcons.FIELD_VAR : JetIcons.FIELD_VAL;
        }

        if (descriptor instanceof TypeParameterDescriptor) {
            return PlatformIcons.CLASS_ICON;
        }

        LOG.warn("No icon for descriptor: " + descriptor);
        return null;
    }
}
