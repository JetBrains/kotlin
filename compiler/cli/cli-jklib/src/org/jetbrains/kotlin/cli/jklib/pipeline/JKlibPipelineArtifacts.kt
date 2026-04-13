/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifactWithExitCode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

data class JKlibFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val projectEnvironment: VfsBasedProjectEnvironment,
    val rootDisposable: Disposable,
) : FrontendPipelineArtifact() {

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }

    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JKlibFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    override val configuration: CompilerConfiguration,
    val frontendOutput: AllModulesFrontendOutput,
    val projectEnvironment: VfsBasedProjectEnvironment,
    val rootDisposable: Disposable,
) : Fir2IrPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JKlibSerializationArtifact(
    val outputKlibPath: String,
    override val configuration: CompilerConfiguration,
    val projectEnvironment: VfsBasedProjectEnvironment,
    val rootDisposable: Disposable,
    override val exitCode: ExitCode = ExitCode.OK,
) : PipelineArtifactWithExitCode() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JKlibIrCompilationArtifact(
    val pluginContext: IrPluginContext,
    val moduleFragment: IrModuleFragment,
    override val configuration: CompilerConfiguration,
    override val exitCode: ExitCode = ExitCode.OK,
) : PipelineArtifactWithExitCode() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}
