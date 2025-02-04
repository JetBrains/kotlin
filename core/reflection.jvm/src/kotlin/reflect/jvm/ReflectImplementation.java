/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm;

import kotlin.reflect.jvm.internal.SystemPropertiesKt;

public enum ReflectImplementation {
    // kotlin.reflect.jvm.useK1Implementation
    DESCRIPTORS,
    // kotlin.reflect.jvm.loadMetadataDirectly
    // kotlin.reflect.jvm.newFakeOverridesImplementation
    METADATA,
    // kotlin.reflect.jvm.newFakeOverridesImplementation
    DESCRIPTORS_WITH_METADATA,
    // Custom
    RAW_PROTOBUF,
    ;

    public static volatile ReflectImplementation CURRENT = DESCRIPTORS;

    public static void set(ReflectImplementation implementation) {
        CURRENT = implementation;
        SystemPropertiesKt.setUseK1Implementation(implementation == DESCRIPTORS);
        SystemPropertiesKt.setLoadMetadataDirectly(implementation == METADATA);
        SystemPropertiesKt.setNewFakeOverridesImplementation(implementation == METADATA || implementation == DESCRIPTORS_WITH_METADATA);
    }
}
