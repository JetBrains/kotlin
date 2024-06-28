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
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.HashMap;
import java.util.Map;

public class JavaDescriptorVisibilities {
    private JavaDescriptorVisibilities() {
    }

    @NotNull
    public static final DescriptorVisibility PACKAGE_VISIBILITY = new DelegatedDescriptorVisibility(JavaVisibilities.PackageVisibility.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptorWithVisibility what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            return areInSamePackage(what, from);
        }

        @Override
        public boolean visibleFromPackage(@NotNull FqName fromPackage, @NotNull FqName myPackage) {
            return fromPackage.equals(myPackage);
        }
    };

    @NotNull
    public static final DescriptorVisibility PROTECTED_STATIC_VISIBILITY = new DelegatedDescriptorVisibility(JavaVisibilities.ProtectedStaticVisibility.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptorWithVisibility what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            return isVisibleForProtectedAndPackage(receiver, what, from);
        }
    };

    @NotNull
    public static final DescriptorVisibility PROTECTED_AND_PACKAGE = new DelegatedDescriptorVisibility(JavaVisibilities.ProtectedAndPackage.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptorWithVisibility what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            return isVisibleForProtectedAndPackage(receiver, what, from);
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

        return DescriptorVisibilities.PROTECTED.isVisible(receiver, what, from, false);
    }

    private static boolean areInSamePackage(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        PackageFragmentDescriptor whatPackage = DescriptorUtils.getParentOfType(first, PackageFragmentDescriptor.class, false);
        PackageFragmentDescriptor fromPackage = DescriptorUtils.getParentOfType(second, PackageFragmentDescriptor.class, false);
        return fromPackage != null && whatPackage != null && whatPackage.getFqName().equals(fromPackage.getFqName());
    }

    @NotNull
    private static final Map<Visibility, DescriptorVisibility> visibilitiesMapping = new HashMap<Visibility, DescriptorVisibility>();

    private static void recordVisibilityMapping(DescriptorVisibility visibility) {
        visibilitiesMapping.put(visibility.getDelegate(), visibility);
    }

    static {
        recordVisibilityMapping(PACKAGE_VISIBILITY);
        recordVisibilityMapping(PROTECTED_STATIC_VISIBILITY);
        recordVisibilityMapping(PROTECTED_AND_PACKAGE);
    }

    @NotNull
    public static DescriptorVisibility toDescriptorVisibility(@NotNull Visibility visibility) {
        DescriptorVisibility correspondingVisibility = visibilitiesMapping.get(visibility);
        if (correspondingVisibility == null) {
            return DescriptorVisibilities.toDescriptorVisibility(visibility);
        }
        return correspondingVisibility;
    }
}
