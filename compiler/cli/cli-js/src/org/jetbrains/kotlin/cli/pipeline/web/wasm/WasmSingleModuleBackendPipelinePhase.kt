/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.*
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePreparationPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.library.isWasmStdlib

object WasmSingleModuleBackendPipelinePhase : WasmBackendPipelinePhase<
        WasmModuleArtifactSingleModule,
        WasmSrcFileArtifactSingleModule,
        WasmIrProgramFragmentsSingleModule,
        WasmICContextSingleModule,
        >() {
    override val icCachePreparationPhase: WebIncrementalCachePreparationPipelinePhase<WasmModuleArtifactSingleModule, *>
        get() = WasmSingleModuleIncrementalCachePreparationPipelinePhase

    override val incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<WasmModuleArtifactSingleModule>, WasmIntermediatePipelineArtifact>
        get() = WasmSingleModuleIncrementalBuildingPhase

    override fun createNonIncrementalCompiler(
        configuration: CompilerConfiguration,
        irFactory: IrFactoryImplForWasmIC,
        module: ModulesStructure
    ): SingleModuleCompiler = SingleModuleCompiler(
        configuration,
        irFactory,
        isWasmStdlib = module.klibs.included?.isWasmStdlib == true,
    )
}
