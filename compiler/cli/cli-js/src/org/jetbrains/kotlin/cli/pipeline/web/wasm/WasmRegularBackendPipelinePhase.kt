/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextWholeWorld
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact

object WasmRegularBackendPipelinePhase : WasmBackendPipelinePhase() {
    override fun compileIncrementally(
        icCaches: List<ModuleArtifact>,
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
