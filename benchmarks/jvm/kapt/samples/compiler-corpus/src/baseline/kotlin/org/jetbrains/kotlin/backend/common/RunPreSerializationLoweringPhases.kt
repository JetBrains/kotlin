/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasureDynamicPhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    lowerings: List<NamedCompilerPhase<T, IrModuleFragment, IrModuleFragment>>,
    irModuleFragment: IrModuleFragment,
): IrModuleFragment {
    return lowerings.fold(irModuleFragment) { module, lowering ->
        context.configuration.perfManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrPreLowering) {
            module.applyIf(!context.diagnosticReporter.hasErrors) {
                runPhase(
                    lowering,
                    module,
                )
            }
        }
    }
}
