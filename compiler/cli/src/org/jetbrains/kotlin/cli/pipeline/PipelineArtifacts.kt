/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

abstract class PipelineArtifact {
    abstract val configuration: CompilerConfiguration

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class CliPipelineInternals(val message: String)

    @CliPipelineInternals(OPT_IN_MESSAGE)
    abstract fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact

    companion object {
        const val OPT_IN_MESSAGE = "This method is intended to be used only by utility `withNewDiagnosticCollector`"
    }
}

abstract class PipelineArtifactWithExitCode : PipelineArtifact() {
    abstract val exitCode: ExitCode
}

data class ArgumentsPipelineArtifact<out A : CommonCompilerArguments>(
    val arguments: A,
    val services: Services,
    val rootDisposable: Disposable,
    val messageCollector: GroupingMessageCollector,
    val performanceManager: PerformanceManager,
) : PipelineArtifact() {
    val diagnosticsCollector: BaseDiagnosticsCollector = DiagnosticsCollectorImpl()
    override val configuration: CompilerConfiguration = CompilerConfiguration.create(messageCollector = messageCollector)

    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        shouldNotBeCalled()
    }
}

data class ConfigurationPipelineArtifact(
    override val configuration: CompilerConfiguration,
    val diagnosticsCollector: BaseDiagnosticsCollector,
    val rootDisposable: Disposable,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): ConfigurationPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

abstract class FrontendPipelineArtifact : PipelineArtifact() {
    abstract val frontendOutput: AllModulesFrontendOutput
    abstract val diagnosticsCollector: BaseDiagnosticsCollector
    abstract override val configuration: CompilerConfiguration
    abstract fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact
}

abstract class Fir2IrPipelineArtifact : PipelineArtifact() {
    abstract val result: Fir2IrActualizedResult
    abstract val diagnosticsCollector: BaseDiagnosticsCollector
    abstract override val configuration: CompilerConfiguration
}

@Suppress("UNCHECKED_CAST")
@OptIn(PipelineArtifact.CliPipelineInternals::class)
fun <A : PipelineArtifact> A.withNewDiagnosticCollector(newDiagnosticsCollector: BaseDiagnosticsCollector): A {
    val newConfiguration = configuration.copy().apply {
        this.diagnosticsCollector = newDiagnosticsCollector
    }
    return withCompilerConfiguration(newConfiguration) as A
}

@Suppress("UNCHECKED_CAST")
fun <A : FrontendPipelineArtifact> A.withNewFrontendOutput(newFrontendOutput: AllModulesFrontendOutput): A =
    withNewFrontendOutputImpl(newFrontendOutput) as A

