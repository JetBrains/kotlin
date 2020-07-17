/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")

package org.jetbrains.kotlin.platform.compat

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform

typealias OldPlatform = org.jetbrains.kotlin.resolve.TargetPlatform
typealias NewPlatform = org.jetbrains.kotlin.platform.TargetPlatform

fun NewPlatform.toOldPlatform(): OldPlatform = when (val single = singleOrNull()) {
    null -> CommonPlatforms.CompatCommonPlatform
    is JvmPlatform -> JvmPlatforms.CompatJvmPlatform
    is JsPlatform -> JsPlatforms.CompatJsPlatform
    is NativePlatform -> NativePlatforms.CompatNativePlatform
    else -> error("Unknown platform $single")
}

fun OldPlatform.toNewPlatform(): NewPlatform = when (this) {
    is CommonPlatforms.CompatCommonPlatform -> this
    is JvmPlatforms.CompatJvmPlatform -> this
    is JsPlatforms.CompatJsPlatform -> this
    is NativePlatforms.CompatNativePlatform -> this
    else -> error(
        "Can't convert org.jetbrains.kotlin.resolve.TargetPlatform to org.jetbrains.kotlin.platform.TargetPlatform: " +
                "non-Compat instance passed\n" +
                "toString: $this\n" +
                "class: ${this::class}\n" +
                "hashCode: ${this.hashCode().toString(16)}"
    )
}

fun IdePlatform<*, *>.toNewPlatform(): NewPlatform = when (this) {
    is CommonIdePlatformKind.Platform -> CommonPlatforms.defaultCommonPlatform
    is JvmIdePlatformKind.Platform -> JvmPlatforms.jvmPlatformByTargetVersion(this.version)
    is JsIdePlatformKind.Platform -> JsPlatforms.defaultJsPlatform
    is NativeIdePlatformKind.Platform -> NativePlatforms.unspecifiedNativePlatform
    else -> error("Unknown platform $this")
}

fun NewPlatform.toIdePlatform(): IdePlatform<*, *> = when (val single = singleOrNull()) {
    null -> CommonIdePlatformKind.Platform
    is JdkPlatform -> JvmIdePlatformKind.Platform(single.targetVersion)
    is JvmPlatform -> JvmIdePlatformKind.Platform(JvmTarget.DEFAULT)
    is JsPlatform -> JsIdePlatformKind.Platform
    is NativePlatform -> NativeIdePlatformKind.Platform
    else -> error("Unknown platform $single")
}
