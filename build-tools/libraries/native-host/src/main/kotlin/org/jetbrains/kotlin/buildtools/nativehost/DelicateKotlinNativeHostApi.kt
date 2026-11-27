/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

/**
 * Marks [KotlinNativeTargetSupport.bundled], the Kotlin/Native support data that ships with this library.
 *
 * The data duplicates what the Kotlin/Native compiler knows (`HostManager` in `kotlin-native-utils`) and matches
 * the Kotlin/Native version this library was released with. It can be wrong if the build uses another
 * Kotlin/Native version.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This data describes the Kotlin/Native version the library was released with." +
            " It can be wrong if the build uses another Kotlin/Native version."
)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class DelicateKotlinNativeHostApi
