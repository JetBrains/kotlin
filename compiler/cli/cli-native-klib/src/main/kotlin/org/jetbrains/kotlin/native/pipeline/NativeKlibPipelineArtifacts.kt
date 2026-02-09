/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.native.Fir2IrOutput
import org.jetbrains.kotlin.native.NativeFirstStagePhaseContext

data class NativeConfigurationArtifact(
    override val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): NativeConfigurationArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class NativeFrontendArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val phaseContext: NativeFirstStagePhaseContext,
) : FrontendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): NativeFrontendArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class NativeFir2IrArtifact(
    val fir2IrOutput: Fir2IrOutput,
    override val configuration: CompilerConfiguration,
    val phaseContext: NativeFirstStagePhaseContext,
) : Fir2IrPipelineArtifact() {
    override val result: Fir2IrActualizedResult
        get() = fir2IrOutput.fir2irActualizedResult

    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): NativeFir2IrArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class NativeSerializationArtifact(
    val serializerOutput: SerializerOutput,
    override val configuration: CompilerConfiguration,
    val phaseContext: NativeFirstStagePhaseContext,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): NativeSerializationArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class NativeKlibSerializedArtifact(
    val outputKlibPath: String,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): NativeKlibSerializedArtifact {
        return copy(configuration = newConfiguration)
    }
}
