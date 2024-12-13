/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.modules.TargetId

@LegacyK2CliPipeline
data class ModuleCompilerInput(
    val targetId: TargetId,
    val groupedSources: GroupedKtSources,
    val configuration: CompilerConfiguration,
)

@LegacyK2CliPipeline
data class ModuleCompilerEnvironment(
    val projectEnvironment: VfsBasedProjectEnvironment,
    val diagnosticsReporter: BaseDiagnosticsCollector
)

@LegacyK2CliPipeline
data class ModuleCompilerOutput(
    val generationState: GenerationState,
    val builderFactory: ClassBuilderFactory
)

data class ModuleCompilerIrBackendInput(
    val targetId: TargetId,
    val configuration: CompilerConfiguration,
    val extensions: JvmFir2IrExtensions,
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizedResult: IrActualizedResult?,
    val symbolTable: SymbolTable,
)
