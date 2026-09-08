/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.*
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePreparationPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.PlatformDependentICContext
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat

abstract class WasmIncrementalCachePreparationPipelinePhase<TModuleArtifact, TIcContext>(
    name: String,
    private val contextFactory: (allowIncompleteImplementations: Boolean, skipLocalNames: Boolean, skipCommentInstructions: Boolean, skipLocations: Boolean) -> TIcContext,
) : WebIncrementalCachePreparationPipelinePhase<TModuleArtifact, TIcContext>(name)
        where TModuleArtifact : ModuleArtifact,
              TIcContext : PlatformDependentICContext<TModuleArtifact, *, *, *> {

    override fun createIcContext(
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): TIcContext = contextFactory(
        /*allowIncompleteImplementations=*/false,
        /*skipLocalNames=*/!configuration.wasmDebug,
        /*skipCommentInstructions=*/!configuration.wasmGenerateWat,
        /*skipLocations=*/!(configuration.wasmGenerateDwarf || configuration.sourceMap),
    )
}

object WasmWholeWorldIncrementalCachePreparationPipelinePhase :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifact, WasmICContextWholeWorld>(
        name = WasmWholeWorldIncrementalCachePreparationPipelinePhase::class.java.simpleName,
        contextFactory = ::WasmICContextWholeWorld,
    )

object WasmSingleModuleIncrementalCachePreparationPipelinePhase :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifactSingleModule, WasmICContextSingleModule>(
        name = WasmSingleModuleIncrementalCachePreparationPipelinePhase::class.java.simpleName,
        contextFactory = ::WasmICContextSingleModule,
    )

object WasmMultiModuleIncrementalCachePreparationPipelinePhase :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifactMultimodule, WasmICContextMultimodule>(
        name = WasmMultiModuleIncrementalCachePreparationPipelinePhase::class.java.simpleName,
        contextFactory = ::WasmICContextMultimodule,
    )
