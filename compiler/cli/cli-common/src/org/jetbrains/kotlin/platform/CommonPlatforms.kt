/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.platform.js.JsPlatforms.allJsPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms.defaultJsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.allJvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.unspecifiedJvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms.allNativePlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms.unspecifiedNativePlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

@Suppress("DEPRECATION_ERROR")
object CommonPlatforms {

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'unspecifiedJvmPlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatCommonPlatform : TargetPlatform(
        setOf(
            unspecifiedJvmPlatform.single(),
            defaultJsPlatform.single(),
            WasmPlatforms.Default.single(),
            unspecifiedNativePlatform.single()
        )
    ), org.jetbrains.kotlin.analyzer.common.CommonPlatform {
        override val platformName: String
            get() = "Default"
    }

    val defaultCommonPlatform: TargetPlatform
        get() = CompatCommonPlatform

    val allSimplePlatforms: List<TargetPlatform>
        // TODO(auskov): migrate to SimplePlatform?
        get() = sequence {
            yieldAll(allJvmPlatforms)
            yieldAll(allNativePlatforms)
            yieldAll(allJsPlatforms)
            yield(WasmPlatforms.Default)

            // TODO(dsavvinov): extensions points?
        }.toList()

    val allDefaultTargetPlatforms: List<TargetPlatform>
        get() = sequence {
            yieldAll(allSimplePlatforms)
            yieldAll(listOf(defaultCommonPlatform))
        }.toList()
}

