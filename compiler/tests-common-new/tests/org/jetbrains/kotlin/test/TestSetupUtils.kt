/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmJsPlatformAnalyzerServices
import java.io.File

/**
 * For proper initialization of idea services those two properties should
 *   be set in environment of test. You can setup them manually via build
 *   system of run configurations or just `initIdeaConfiguration` before
 *   running tests using abilities of core test framework you use
 */
fun initIdeaConfiguration() {
    System.setProperty("idea.home", computeHomeDirectory())
    System.setProperty("idea.ignore.disabled.plugins", "true")
}

private fun computeHomeDirectory(): String {
    val userDir = System.getProperty("user.dir")
    return File(userDir ?: ".").canonicalPath
}

fun TargetPlatform.getAnalyzerServices(): PlatformDependentAnalyzerServices {
    return when {
        isJvm() -> JvmPlatformAnalyzerServices
        isJs() -> JsPlatformAnalyzerServices
        isNative() -> NativePlatformAnalyzerServices
        isCommon() -> CommonPlatformAnalyzerServices
        isWasm() -> WasmJsPlatformAnalyzerServices
        else -> error("Unknown target platform: $this")
    }
}