/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifactWithExitCode
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.name.FqName

class JvmScriptPipelineArtifact(override val exitCode: ExitCode) : PipelineArtifactWithExitCode()

data class JvmFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val sourceFiles: List<KtSourceFile>,
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector) =
        copy(diagnosticCollector = newDiagnosticsCollector)
}

data class JvmFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val sourceFiles: List<KtSourceFile>,
    val mainClassFqName: FqName?,
) : Fir2IrPipelineArtifact()

data class JvmBackendPipelineArtifact(
    val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val mainClassFqName: FqName?,
    val outputs: List<GenerationState>,
) : PipelineArtifact()

class JvmBinaryPipelineArtifact(val outputs: List<GenerationState>) : PipelineArtifact()
