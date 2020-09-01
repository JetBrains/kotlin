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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

public class JavaDescriptorVisibilities {
    private JavaDescriptorVisibilities() {
    }

    @NotNull
    public static final DescriptorVisibility PACKAGE_VISIBILITY = new DescriptorVisibility("package", false) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return areInSamePackage(what, from);
        }

        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        protected Integer compareTo(@NotNull DescriptorVisibility visibility) {
            if (this == visibility) return 0;
            if (DescriptorVisibilities.isPrivate(visibility)) return 1;
            return -1;
        }

        @NotNull
        @Override
        public String getInternalDisplayName() {
            return "public/*package*/";
        }

        @NotNull
        @Override
        public String getExternalDisplayName() {
            return "package-private";
        }

        @NotNull
        @Override
        public DescriptorVisibility normalize() {
            return DescriptorVisibilities.PROTECTED;
        }

        @Nullable
        @Override
        public EffectiveVisibility customEffectiveVisibility() {
            return EffectiveVisibility.PackagePrivate.INSTANCE;
        }
    };

    @NotNull
    public static final DescriptorVisibility PROTECTED_STATIC_VISIBILITY = new DescriptorVisibility("protected_static", true) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return isVisibleForProtectedAndPackage(receiver, what, from);
        }

        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @NotNull
        @Override
        public String getInternalDisplayName() {
            return "protected/*protected static*/";
        }

        @NotNull
        @Override
        public String getExternalDisplayName() {
            return "protected";
        }

        @NotNull
        @Override
        public DescriptorVisibility normalize() {
            return DescriptorVisibilities.PROTECTED;
        }
    };

    @NotNull
    public static final DescriptorVisibility PROTECTED_AND_PACKAGE = new DescriptorVisibility("protected_and_package", true) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return isVisibleForProtectedAndPackage(receiver, what, from);
        }

        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        protected Integer compareTo(@NotNull DescriptorVisibility visibility) {
            if (this == visibility) return 0;
            if (visibility == DescriptorVisibilities.INTERNAL) return null;
            if (DescriptorVisibilities.isPrivate(visibility)) return 1;
            return -1;
        }

        @NotNull
        @Override
        public String getInternalDisplayName() {
            return "protected/*protected and package*/";
        }

        @NotNull
        @Override
        public String getExternalDisplayName() {
            return "protected";
        }

        @NotNull
        @Override
        public DescriptorVisibility normalize() {
            return DescriptorVisibilities.PROTECTED;
        }
    };

    private static boolean isVisibleForProtectedAndPackage(
            @Nullable ReceiverValue receiver,
            @NotNull DeclarationDescriptorWithVisibility what,
            @NotNull DeclarationDescriptor from
    ) {
        if (areInSamePackage(DescriptorUtils.unwrapFakeOverrideToAnyDeclaration(what), from)) {
            return true;
        }

        return DescriptorVisibilities.PROTECTED.isVisible(receiver, what, from);
    }

    private static boolean areInSamePackage(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        PackageFragmentDescriptor whatPackage = DescriptorUtils.getParentOfType(first, PackageFragmentDescriptor.class, false);
        PackageFragmentDescriptor fromPackage = DescriptorUtils.getParentOfType(second, PackageFragmentDescriptor.class, false);
        return fromPackage != null && whatPackage != null && whatPackage.getFqName().equals(fromPackage.getFqName());
    }
}
