/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.ic.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure

object WasmRegularBackendPipelinePhase : WasmBackendPipelinePhase<
        WasmModuleArtifact,
        WasmSrcFileArtifact,
        WasmIrProgramFragments,
        WasmICContextWholeWorld,
        >() {
    override fun compileIncrementally(
        icCaches: List<WasmModuleArtifact>,
        configuration: CompilerConfiguration
    ): List<WasmIrModuleConfiguration> = compileIncrementallyWholeWorld(icCaches, configuration)

    override fun createIcContext(
        allowIncompleteImplementations: Boolean,
        skipLocalNames: Boolean,
        skipCommentInstructions: Boolean,
        skipLocations: Boolean
    ): WasmICContextWholeWorld = WasmICContextWholeWorld(
        allowIncompleteImplementations,
        skipLocalNames,
        skipCommentInstructions,
        skipLocations,
    )

    override fun createNonIncrementalCompiler(
        configuration: CompilerConfiguration,
        irFactory: IrFactoryImplForWasmIC,
        module: ModulesStructure,
    ): WholeWorldCompiler = WholeWorldCompiler(configuration, irFactory)
}
