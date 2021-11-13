/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import java.io.File

// ---

data class ModuleCompilerInput(
    val targetId: TargetId,
    val commonPlatform: TargetPlatform,
    val commonSources: Collection<File>,
    val platform: TargetPlatform,
    val platformSources: Collection<File>,
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

// ---

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

data class ModuleCompilerIrBackendInput(
    val targetId: TargetId,
    val configuration: CompilerConfiguration,
    val extensions: JvmGeneratorExtensionsImpl,
    val irModuleFragment: IrModuleFragment,
    val symbolTable: SymbolTable,
    val components: Fir2IrComponents,
    val firSession: FirSession
)