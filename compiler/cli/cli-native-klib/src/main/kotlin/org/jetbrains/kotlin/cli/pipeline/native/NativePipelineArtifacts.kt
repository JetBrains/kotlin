/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.native.Fir2IrOutput
import org.jetbrains.kotlin.native.FirOutput

data class NativeFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val firOutput: FirOutput.Full,
    val environment: KotlinCoreEnvironment,
    val phaseContext: NativeKlibPhaseContext,
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): NativeFrontendPipelineArtifact {
        return copy(diagnosticCollector = newDiagnosticsCollector)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class NativeFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val fir2IrOutput: Fir2IrOutput,
    val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
    val phaseContext: NativeKlibPhaseContext,
) : Fir2IrPipelineArtifact()

data class NativeSerializedKlibPipelineArtifact(
    val serializerOutput: SerializerOutput,
    val outputKlibPath: String,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val configuration: CompilerConfiguration,
) : PipelineArtifact()
