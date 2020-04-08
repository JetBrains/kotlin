/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KonanPlatformKt")

package org.jetbrains.kotlin.platform.konan

import org.jetbrains.kotlin.platform.TargetPlatform

// for backward compatibility with kotlin-serialization-compiler runtime
@Deprecated(
    message = "This class is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
    replaceWith = ReplaceWith("org.jetbrains.kotlin.platform.konan.NativePlatformKt.isNative"),
    level = DeprecationLevel.ERROR
)
@JvmName("isNative")
fun TargetPlatform.deprecatedIsNative(): Boolean = isNative()
