/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.container.DefaultImplementation;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;

import java.util.Collection;
import java.util.Collections;

@DefaultImplementation(impl = PlatformToKotlinClassMap.Default.class)
public interface PlatformToKotlinClassMap {
    @NotNull
    Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor);

    class Default implements PlatformToKotlinClassMap {
        @NotNull
        @Override
        public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
            return Collections.emptyList();
        }
    }
}
