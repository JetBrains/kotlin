/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.native.fir2Ir

object NativeFir2IrPipelinePhase : PipelinePhase<NativeFrontendPipelineArtifact, NativeFir2IrPipelineArtifact>(
    name = "NativeFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFrontendPipelineArtifact): NativeFir2IrPipelineArtifact? {
        val fir2IrOutput = input.phaseContext.fir2Ir(input.firOutput)

        // Run native interop checks.
        // Should be replaced by frontend checks at some point.
        // TODO: add KT ticket.
        val moduleFragment = fir2IrOutput.fir2irActualizedResult.irModuleFragment
        SpecialBackendChecksTraversal(
            input.phaseContext,
            fir2IrOutput.symbols,
            fir2IrOutput.fir2irActualizedResult.irBuiltIns,
        ).lower(moduleFragment)

        return NativeFir2IrPipelineArtifact(
            result = fir2IrOutput.fir2irActualizedResult,
            diagnosticCollector = input.diagnosticCollector,
            fir2IrOutput = fir2IrOutput,
            configuration = input.configuration,
            environment = input.environment,
            phaseContext = input.phaseContext,
        )
    }
}
