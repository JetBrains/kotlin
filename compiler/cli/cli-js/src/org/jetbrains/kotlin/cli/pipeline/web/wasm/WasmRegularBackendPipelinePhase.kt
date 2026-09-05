/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextWholeWorld
import org.jetbrains.kotlin.backend.wasm.ic.WasmIrProgramFragments
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ic.WasmSrcFileArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePreparationPipelinePhase

object WasmRegularBackendPipelinePhase : WasmBackendPipelinePhase<
        WasmModuleArtifact,
        WasmSrcFileArtifact,
        WasmIrProgramFragments,
        WasmICContextWholeWorld,
        >() {
    override val icCachePreparationPhase: WebIncrementalCachePreparationPipelinePhase<WasmModuleArtifact, *>
        get() = WasmWholeWorldIncrementalCachePreparationPipelinePhase

    override val incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<WasmModuleArtifact>, WasmIntermediatePipelineArtifact>
        get() = WasmWholeWorldIncrementalBuildingPhase

    override val backendIrGenerationPhase: WasmBackendIrGenerationPipelinePhase
        get() = WasmWholeWorldBackendIrGenerationPipelinePhase
}
