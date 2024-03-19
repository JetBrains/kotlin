/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatformUnspecifiedTarget
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatformUnspecifiedTarget
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget

enum class TargetPlatformEnum(val targetPlatform: TargetPlatform) {
    Common(
        TargetPlatform(
            setOf(
                JdkPlatform(JvmTarget.DEFAULT),
                JsPlatforms.DefaultSimpleJsPlatform,
                WasmPlatformWithTarget(WasmTarget.JS),
                WasmPlatformWithTarget(WasmTarget.WASI),
                NativePlatformUnspecifiedTarget
            )
        )
    ),

    JVM(JvmPlatforms.unspecifiedJvmPlatform),
    JVM_1_6(JvmPlatforms.jvm6),
    JVM_1_8(JvmPlatforms.jvm8),

    JS(JsPlatforms.defaultJsPlatform),
    Wasm(WasmPlatforms.wasmJs),
    Native(NativePlatforms.unspecifiedNativePlatform)
}
