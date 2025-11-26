/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.native.Fir2IrOutput

/**
 * Artifact produced by the configuration phase.
 * Contains the phase context and configuration needed for subsequent phases.
 */
data class NativeKlibConfigurationArtifact(
    val phaseContext: PhaseContext,
    val environment: KotlinCoreEnvironment,
    override val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
) : FrontendPipelineArtifact() {
    // FrontendPipelineArtifact requires frontendOutput, but we don't have it yet at this stage
    override val frontendOutput: AllModulesFrontendOutput
        get() = throw IllegalStateException("Frontend output is not available at configuration stage")

    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): FrontendPipelineArtifact {
        return copy(diagnosticCollector = newDiagnosticsCollector)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        throw IllegalStateException("Cannot set frontend output at configuration stage")
    }
}

/**
 * Artifact produced by the frontend phase.
 */
data class NativeKlibFrontendArtifact(
    val phaseContext: PhaseContext,
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): FrontendPipelineArtifact {
        return copy(diagnosticCollector = newDiagnosticsCollector)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

/**
 * Artifact produced by the Fir2Ir phase.
 */
data class NativeKlibFir2IrArtifact(
    val phaseContext: PhaseContext,
    override val result: Fir2IrActualizedResult,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val fir2IrOutput: Fir2IrOutput,
) : Fir2IrPipelineArtifact()

/**
 * Artifact produced by the serializer phase.
 */
data class NativeKlibSerializerArtifact(
    val phaseContext: PhaseContext,
    val serializerOutput: SerializerOutput,
    val outputKlibPath: String,
) : PipelineArtifact()
