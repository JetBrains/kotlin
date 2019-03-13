/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.container.PlatformSpecificExtension;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;

import java.util.Collection;
import java.util.Collections;

public interface PlatformToKotlinClassMap extends PlatformSpecificExtension<PlatformToKotlinClassMap> {
    PlatformToKotlinClassMap EMPTY = new PlatformToKotlinClassMap() {
        @NotNull
        @Override
        public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
            return Collections.emptyList();
        }
    };

    @NotNull
    Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor);
}

