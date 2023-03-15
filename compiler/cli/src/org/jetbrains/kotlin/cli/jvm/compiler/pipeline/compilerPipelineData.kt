/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizationResult
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.platform.TargetPlatform

// ---

data class ModuleCompilerInput(
    val targetId: TargetId,
    val groupedSources: GroupedKtSources,
    val commonPlatform: TargetPlatform,
    val platform: TargetPlatform,
    val configuration: CompilerConfiguration,
    val friendFirModules: Collection<FirModuleData> = emptyList()
)

data class ModuleCompilerEnvironment(
    val projectEnvironment: AbstractProjectEnvironment,
    val diagnosticsReporter: BaseDiagnosticsCollector
)

data class ModuleCompilerOutput(
    val generationState: GenerationState
)

data class ModuleCompilerIrBackendInput(
    val targetId: TargetId,
    val configuration: CompilerConfiguration,
    val extensions: JvmFir2IrExtensions,
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizationResult: IrActualizationResult?
)
