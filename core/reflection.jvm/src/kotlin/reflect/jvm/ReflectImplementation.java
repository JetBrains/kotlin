/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm;

public enum ReflectImplementation {
    DESCRIPTORS,
    METADATA,
    DESCRIPTORS_WITH_METADATA,
    RAW_PROTOBUF,
    ;

    public static volatile ReflectImplementation CURRENT = DESCRIPTORS;
}
