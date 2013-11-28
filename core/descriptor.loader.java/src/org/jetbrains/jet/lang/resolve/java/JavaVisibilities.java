/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;

public class JavaVisibilities {
    private JavaVisibilities() {
    }

    public static final Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return areInSamePackage(what, from);
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.PRIVATE) return 1;
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

    public static final Visibility PROTECTED_STATIC_VISIBILITY = new Visibility("protected_static", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            ClassDescriptor whatClass;
            // protected static class
            if (what instanceof ClassDescriptor) {
                DeclarationDescriptor containingDeclaration = what.getContainingDeclaration();
                assert containingDeclaration instanceof ClassDescriptor : "Only static nested classes can have protected_static visibility";
                whatClass = (ClassDescriptor) containingDeclaration;
            }
            // protected static function or property
            else {
                DeclarationDescriptor whatDeclarationDescriptor = what.getContainingDeclaration();
                assert whatDeclarationDescriptor instanceof JavaPackageFragmentDescriptor : "Only static declarations can have protected_static visibility";
                whatClass = DescriptorResolverUtils
                        .getClassForCorrespondingJavaPackage((JavaPackageFragmentDescriptor) whatDeclarationDescriptor);
            }

            assert whatClass != null : "Couldn't find ClassDescriptor for protected static member " + what;

            if (DescriptorUtils.isSubclass(fromClass, whatClass)) {
                return true;
            }
            return isVisible(what, fromClass.getContainingDeclaration());
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

    public static final Visibility PROTECTED_AND_PACKAGE = new Visibility("protected_and_package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
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
            return isVisible(what, fromClass.getContainingDeclaration());
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.INTERNAL) return null;
            if (visibility == Visibilities.PRIVATE) return 1;
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
