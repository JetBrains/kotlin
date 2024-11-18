/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult

abstract class PipelineArtifact

abstract class PipelineArtifactWithExitCode : PipelineArtifact() {
    abstract val exitCode: ExitCode
}

data class ArgumentsPipelineArtifact<out A : CommonCompilerArguments>(
    val arguments: A,
    val services: Services,
    val rootDisposable: Disposable,
    val messageCollector: GroupingMessageCollector,
    val performanceManager: CommonCompilerPerformanceManager,
) : PipelineArtifact() {
    val diagnosticCollector: BaseDiagnosticsCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)
}

data class ConfigurationPipelineArtifact(
    val configuration: CompilerConfiguration,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val rootDisposable: Disposable,
) : PipelineArtifact()

abstract class FrontendPipelineArtifact : PipelineArtifact() {
    abstract val result: FirResult
}

abstract class Fir2IrPipelineArtifact : PipelineArtifact() {
    abstract val result: Fir2IrActualizedResult
}

