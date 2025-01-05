/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.checkers.IrInlineDeclarationChecker
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.PreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.ir.inline.runIrLevelCheckers

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    irModuleFragment: IrModuleFragment,
    loweringProvider: PreSerializationLoweringPhasesProvider<T>,
    diagnosticReporter: BaseDiagnosticsCollector,
): IrModuleFragment {
    val moduleFragment = runPhase(
        loweringProvider.lowerings(),
        irModuleFragment,
        disable = !this.context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization),
    )
    irModuleFragment.runIrLevelCheckers(
        KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter.deduplicating(), context.configuration.languageVersionSettings),
        ::IrInlineDeclarationChecker,
    )
    return moduleFragment
}

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    fir2IrActualizedResult: Fir2IrActualizedResult,
    loweringProvider: PreSerializationLoweringPhasesProvider<T>,
    diagnosticReporter: BaseDiagnosticsCollector,
): Fir2IrActualizedResult = fir2IrActualizedResult.copy(
    irModuleFragment = runPreSerializationLoweringPhases(
        fir2IrActualizedResult.irModuleFragment,
        loweringProvider,
        diagnosticReporter,
    )
)
