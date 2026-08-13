/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.js.config.JsGenerationGranularity

sealed class IcCachesConfigurationData {
    data class Js(
        val granularity: JsGenerationGranularity,
    ) : IcCachesConfigurationData()

    data class Wasm(
        val wasmDebug: Boolean,
        val generateWat: Boolean,
        val generateDebugInformation: Boolean,
        val mode: WasmCompilationMode,
    ) : IcCachesConfigurationData()
}

typealias IcCachesArtifacts = List<ModuleArtifact>
