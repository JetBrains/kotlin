/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.konan.NativePreSerializationLoweringContext
import org.jetbrains.kotlin.backend.konan.driver.NativePhaseContext
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.inline.konan.nativeLoweringsOfTheFirstPhase

internal val testProcessorModulePhase = makeIrModulePhase(
    lowering = ::TestProcessor,
    name = "TestProcessor",
)

public fun <T : NativePhaseContext> PhaseEngine<T>.runPreSerializationLowerings(fir2IrOutput: Fir2IrOutput, environment: KotlinCoreEnvironment): Fir2IrOutput {
    val diagnosticReporter = DiagnosticsCollectorImpl()
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        diagnosticReporter,
        environment.configuration.languageVersionSettings
    )
    val loweringContext = NativePreSerializationLoweringContext(
        fir2IrOutput.fir2irActualizedResult.irBuiltIns,
        environment.configuration,
        irDiagnosticReporter,
    )
    val preSerializationLowered = newEngine(loweringContext) { engine ->
        // TODO: move to nativeLoweringsOfTheFirstPhase after they moved to NativeLoweringPhases.kt
        // Unfortunately, this needs K/N to be turned on by default in the Kotlin repository.
        val lowerings = buildList {
            val runTestProcessorModulePhase =
                environment.configuration.languageVersionSettings.supportsFeature(LanguageFeature.NativeTestProcessorBeforeSerialization)
            if (runTestProcessorModulePhase) add(testProcessorModulePhase)
            addAll(nativeLoweringsOfTheFirstPhase(environment.configuration.languageVersionSettings))
        }
        engine.runPreSerializationLoweringPhases(
            fir2IrOutput.fir2irActualizedResult,
            lowerings,
        )
    }
    // TODO: After KT-73624, generate native diagnostic tests for `compiler/testData/diagnostics/irInliner/syntheticAccessors`
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
        diagnosticReporter,
        environment.configuration.messageCollector,
        environment.configuration.renderDiagnosticInternalName,
    )
    if (diagnosticReporter.hasErrors) {
        throw CompilationErrorException("Compilation failed: there were some diagnostics during IR Inliner")
    }

    return fir2IrOutput.copy(
        fir2irActualizedResult = preSerializationLowered,
    )
}
