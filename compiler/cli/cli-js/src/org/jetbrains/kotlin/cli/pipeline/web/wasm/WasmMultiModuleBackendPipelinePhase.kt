/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragmentsMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmSrcFileArtifactMultimodule
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePreparationPipelinePhase

object WasmMultiModuleBackendPipelinePhase : WasmBackendPipelinePhase<
        WasmModuleArtifactMultimodule,
        WasmSrcFileArtifactMultimodule,
        WasmIrProgramFragmentsMultimodule,
        WasmICContextMultimodule
        >() {
    override val icCachePreparationPhase: WebIncrementalCachePreparationPipelinePhase<WasmModuleArtifactMultimodule, *>
        get() = WasmMultiModuleIncrementalCachePreparationPipelinePhase

    override val incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<WasmModuleArtifactMultimodule>, WasmIntermediatePipelineArtifact>
        get() = WasmMultiModuleIncrementalBuildingPhase

    override val backendIrGenerationPhase: WasmBackendIrGenerationPipelinePhase
        get() = WasmMultiModuleBackendIrGenerationPipelinePhase
}
