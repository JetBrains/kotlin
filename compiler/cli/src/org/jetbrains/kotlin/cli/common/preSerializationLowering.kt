/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    lowerings: List<SimpleNamedCompilerPhase<T, IrModuleFragment, IrModuleFragment>>,
    irModuleFragment: IrModuleFragment,
): IrModuleFragment {
    return lowerings.fold(irModuleFragment) { module, lowering ->
        runPhase(
            lowering,
            module,
            disable = !this.context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization),
        )
    }
}

fun <T : PreSerializationLoweringContext> PhaseEngine<T>.runPreSerializationLoweringPhases(
    fir2IrActualizedResult: Fir2IrActualizedResult,
    lowerings: List<SimpleNamedCompilerPhase<T, IrModuleFragment, IrModuleFragment>>
): Fir2IrActualizedResult = fir2IrActualizedResult.copy(
    irModuleFragment = runPreSerializationLoweringPhases(
        lowerings,
        fir2IrActualizedResult.irModuleFragment,
    )
)
