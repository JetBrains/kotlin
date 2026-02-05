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
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.native.Fir2IrOutput
import org.jetbrains.kotlin.native.NativeFirstStagePhaseContext

data class NativeConfigurationArtifact(
    override val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
    val diagnosticsCollector: BaseDiagnosticsCollector,
) : PipelineArtifact()

data class NativeFrontendArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
    override val diagnosticsCollector: BaseDiagnosticsCollector,
    val phaseContext: NativeFirstStagePhaseContext,
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): NativeFrontendArtifact {
        return copy(diagnosticsCollector = newDiagnosticsCollector)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class NativeFir2IrArtifact(
    val fir2IrOutput: Fir2IrOutput,
    override val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
    override val diagnosticsCollector: BaseDiagnosticsCollector,
    val phaseContext: NativeFirstStagePhaseContext,
) : Fir2IrPipelineArtifact() {
    override val result: Fir2IrActualizedResult
        get() = fir2IrOutput.fir2irActualizedResult
}

data class NativeSerializationArtifact(
    val serializerOutput: SerializerOutput,
    override val configuration: CompilerConfiguration,
    val diagnosticsCollector: BaseDiagnosticsCollector,
    val phaseContext: NativeFirstStagePhaseContext,
) : PipelineArtifact()

data class NativeKlibSerializedArtifact(
    val outputKlibPath: String,
    override val configuration: CompilerConfiguration,
    val diagnosticsCollector: BaseDiagnosticsCollector,
) : PipelineArtifact()
