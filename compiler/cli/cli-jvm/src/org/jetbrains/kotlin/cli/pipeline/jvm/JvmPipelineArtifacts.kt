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
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.name.FqName

data class JvmScriptPipelineArtifact(
    override val exitCode: ExitCode,
    override val configuration: CompilerConfiguration,
) : PipelineArtifactWithExitCode() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JvmScriptPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JvmFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    val sourceFiles: List<KtSourceFile>,
) : FrontendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JvmFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class JvmFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    override val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    val sourceFiles: List<KtSourceFile>,
    val mainClassFqName: FqName?,
) : Fir2IrPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JvmFir2IrPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JvmBackendPipelineArtifact(
    override val configuration: CompilerConfiguration,
    val environment: VfsBasedProjectEnvironment,
    val mainClassFqName: FqName?,
    val outputs: List<GenerationState>,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JvmBackendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JvmBinaryPipelineArtifact(
    val outputs: List<GenerationState>,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JvmBinaryPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}
