/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.cli.pipeline.web.WebIrLoadingPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.loadIrForSingleModule
import org.jetbrains.kotlin.ir.declarations.IrFactory

object WasmIrLoadingPipelinePhase : WebIrLoadingPipelinePhase("WasmIrLoadingPipelinePhase") {
    override fun createIrFactory(): IrFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

    override fun loadIr(configuration: CompilerConfiguration, irFactory: IrFactory, modulesStructure: ModulesStructure): IrModuleInfo =
        when (configuration.wasmCompilationMode()) {
            WasmCompilationMode.SINGLE_MODULE -> loadIrForSingleModule(modulesStructure, irFactory)
            WasmCompilationMode.MULTI_MODULE, WasmCompilationMode.REGULAR -> super.loadIr(configuration, irFactory, modulesStructure)
        }
}
