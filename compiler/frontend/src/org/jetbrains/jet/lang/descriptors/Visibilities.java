/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.Set;

/**
 * @author svtk
 */
public class Visibilities {
    public static final Visibility PRIVATE = new Visibility(false) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            DeclarationDescriptor parent = what;
            while (parent != null) {
                parent = parent.getContainingDeclaration();
                if ((parent instanceof ClassDescriptor && ((ClassDescriptor)parent).getKind() != ClassKind.OBJECT) ||
                    parent instanceof NamespaceDescriptor) {
                    break;
                }
            }
            DeclarationDescriptor fromParent = from;
            while (fromParent != null) {
                if (parent == fromParent) {
                    return true;
                }
                if (fromParent instanceof NamespaceDescriptor) {
                    break; //'private' package members are not visible for subpackages, so when we reach a package, we should stop
                }
                fromParent = fromParent.getContainingDeclaration();
            }
            return false;
        }
    };

    public static final Visibility PROTECTED = new Visibility(true) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            ClassDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);
            if (classDescriptor == null) return false;

            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class);
            if (fromClass == null) return false;
            if (DescriptorUtils.isSubclass(fromClass, classDescriptor)) {
                return true;
            }
            return false;
        }
    };

    public static final Visibility INTERNAL = new Visibility(false) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            ModuleDescriptor parentModule = DescriptorUtils.getParentOfType(what, ModuleDescriptor.class, false);
            ModuleDescriptor fromModule = DescriptorUtils.getParentOfType(from, ModuleDescriptor.class, false);
            return parentModule == fromModule;
        }
    };

    public static final Visibility PUBLIC = new Visibility(true) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            return true;
        }
    };

    public static final Visibility INTERNAL_PROTECTED = new Visibility(false) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            return PROTECTED.isVisible(what, from) && INTERNAL.isVisible(what, from);
        }
    };

    public static final Visibility LOCAL = new Visibility(false) {
        @Override
        protected boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
            return true;
        }
    };

    public static final Set<Visibility> INTERNAL_VISIBILITIES = Sets.newHashSet(PRIVATE, INTERNAL, INTERNAL_PROTECTED, LOCAL);

    public static boolean isVisible(DeclarationDescriptorWithVisibility what, DeclarationDescriptor from) {
        DeclarationDescriptorWithVisibility parent = what;
        while (parent != null) {
            if (!parent.getVisibility().isVisible(parent, from)) {
                return false;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }
        return true;
    }
}
