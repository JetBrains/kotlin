/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasureDynamicPhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    lowerings: List<NamedCompilerPhase<T, IrModuleFragment, IrModuleFragment>>,
    irModuleFragment: IrModuleFragment,
): IrModuleFragment {
    val diagnosticReporter = context.diagnosticReporter as? BaseDiagnosticsCollector
    return lowerings.fold(irModuleFragment) { module, lowering ->
        context.configuration.perfManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrPreLowering) {
            module.applyIf(diagnosticReporter?.hasErrors != true) {
                runPhase(
                    lowering,
                    module,
                )
            }
        }
    }
}

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    fir2IrActualizedResult: Fir2IrActualizedResult,
    lowerings: List<NamedCompilerPhase<T, IrModuleFragment, IrModuleFragment>>
): Fir2IrActualizedResult = fir2IrActualizedResult.copy(
    irModuleFragment = runPreSerializationLoweringPhases(
        lowerings,
        fir2IrActualizedResult.irModuleFragment,
    )
)
