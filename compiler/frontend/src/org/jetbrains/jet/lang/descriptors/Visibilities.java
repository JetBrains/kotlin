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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author svtk
 */
public class Visibilities {
    public static final Visibility PRIVATE = new Visibility("private", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
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

    public static final Visibility PROTECTED = new Visibility("protected", true) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
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

    public static final Visibility INTERNAL = new Visibility("internal", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            ModuleDescriptor parentModule = DescriptorUtils.getParentOfType(what, ModuleDescriptor.class, false);
            ModuleDescriptor fromModule = DescriptorUtils.getParentOfType(from, ModuleDescriptor.class, false);
            return parentModule == fromModule;
        }
    };

    public static final Visibility PUBLIC = new Visibility("public", true) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return true;
        }
    };

    public static final Visibility INTERNAL_PROTECTED = new Visibility("internal protected", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return PROTECTED.isVisible(what, from) && INTERNAL.isVisible(what, from);
        }
    };

    public static final Visibility LOCAL = new Visibility("local", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
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

    private static final Map<Visibility, Integer> ORDERED_VISIBILITIES = Maps.newHashMap();
    static {
        ORDERED_VISIBILITIES.put(PRIVATE, 0);
        ORDERED_VISIBILITIES.put(INTERNAL_PROTECTED, 1);
        ORDERED_VISIBILITIES.put(INTERNAL, 2);
        ORDERED_VISIBILITIES.put(PROTECTED, 2);
        ORDERED_VISIBILITIES.put(PUBLIC, 3);
    }

    /*package*/ static Integer compareLocal(@NotNull Visibility first, @NotNull Visibility second) {
        if (first == second) return 0;
        Integer firstIndex = ORDERED_VISIBILITIES.get(first);
        Integer secondIndex = ORDERED_VISIBILITIES.get(second);
        if (firstIndex == null || secondIndex == null || firstIndex.equals(secondIndex)) {
            return null;
        }
        return firstIndex - secondIndex;
    }

    public static Integer compare(@NotNull Visibility first, @NotNull Visibility second) {
        Integer result = first.compareTo(second);
        if (result != null) {
            return result;
        }
        Integer oppositeResult = second.compareTo(first);
        if (oppositeResult != null) {
            return -oppositeResult;
        }
        return null;
    }
}
