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

package org.jetbrains.kotlin.load.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

public class JavaVisibilities {
    private JavaVisibilities() {
    }

    public static final Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return areInSamePackage(what, from);
        }

        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (Visibilities.isPrivate(visibility)) return 1;
            return -1;
        }

        @Override
        public String toString() {
            return "public/*package*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.INTERNAL;
        }
    };

    public static final Visibility PROTECTED_STATIC_VISIBILITY = new Visibility("protected_static", true) {
        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (areInSamePackage(what, from)) {
                return true;
            }

            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            DeclarationDescriptor containingDeclaration = what.getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor : "Only class members can have protected_static visibility";
            ClassDescriptor whatClass = (ClassDescriptor) containingDeclaration;

            if (DescriptorUtils.isSubclass(fromClass, whatClass)) {
                return true;
            }
            return isVisible(receiver, what, fromClass.getContainingDeclaration());
        }

        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        public String toString() {
            return "protected/*protected static*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.PROTECTED;
        }
    };

    public static final Visibility PROTECTED_AND_PACKAGE = new Visibility("protected_and_package", true) {
        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (areInSamePackage(what, from)) {
                return true;
            }

            ClassDescriptor whatClass = DescriptorUtils.getParentOfType(what, ClassDescriptor.class, false);
            if (whatClass == null) return false;

            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            if (DescriptorUtils.isSubclass(fromClass, whatClass)) {
                return true;
            }
            return isVisible(receiver, what, fromClass.getContainingDeclaration());
        }

        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.INTERNAL) return null;
            if (Visibilities.isPrivate(visibility)) return 1;
            return -1;
        }

        @Override
        public String toString() {
            return "protected/*protected and package*/";
        }

        @NotNull
        @Override
        public Visibility normalize() {
            return Visibilities.PROTECTED;
        }
    };

    private static boolean areInSamePackage(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        PackageFragmentDescriptor whatPackage = DescriptorUtils.getParentOfType(first, PackageFragmentDescriptor.class, false);
        PackageFragmentDescriptor fromPackage = DescriptorUtils.getParentOfType(second, PackageFragmentDescriptor.class, false);
        return fromPackage != null && whatPackage != null && whatPackage.getFqName().equals(fromPackage.getFqName());
    }
}
