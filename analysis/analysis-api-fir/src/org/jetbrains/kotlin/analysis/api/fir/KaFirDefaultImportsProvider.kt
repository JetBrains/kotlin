/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.impl.base.imports.KaBaseDefaultImportsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getWasmTarget
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.fir.resolve.FirJvmDefaultImportProvider
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.WasmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.resolve.DefaultImportProvider
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmPlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmWasiPlatformAnalyzerServices

// K1 implementation is in IDE: `org.jetbrains.kotlin.base.fe10.analysis.KaFe10DefaultImportsProvider`
internal class KaFirDefaultImportsProvider : KaBaseDefaultImportsProvider() {
    override fun getCompilerDefaultImportProvider(targetPlatform: TargetPlatform): DefaultImportProvider = when {
        targetPlatform.all { it is JvmPlatform } -> FirJvmDefaultImportProvider
        targetPlatform.all { it is JsPlatform } -> JsPlatformAnalyzerServices
        targetPlatform.all { it is NativePlatform } -> NativePlatformAnalyzerServices
        targetPlatform.all { it is WasmPlatform } -> when (targetPlatform.getWasmTarget()) {
            WasmTarget.JS -> WasmPlatformAnalyzerServices
            WasmTarget.WASI -> WasmWasiPlatformAnalyzerServices
        }
        targetPlatform.all { (it as? WasmPlatformWithTarget)?.target == WasmTarget.JS || it is JsPlatform } -> JsPlatformAnalyzerServices
        else -> CommonPlatformAnalyzerServices
    }
}