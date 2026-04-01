/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline.konan

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.phaser.createModulePhases
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.loweringsOfTheFirstPhase

private fun createUpgradeCallableReferencesPhase(context: LoweringContext): UpgradeCallableReferences {
    return UpgradeCallableReferences(context)
}

private val upgradeCallableReferencesPhase = makeIrModulePhase(
    lowering = ::UpgradeCallableReferences,
    name = "UpgradeCallableReferences"
)

private val assertionWrapperPhase = makeIrModulePhase(
    lowering = ::NativeAssertionWrapperLowering,
    name = "AssertionWrapperLowering",
)

fun nativeLoweringsOfTheFirstPhase(
    languageVersionSettings: LanguageVersionSettings,
): List<NamedCompilerPhase<PreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
    val phases = buildList<(PreSerializationLoweringContext) -> ModuleLoweringPass> {
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs)) {
            this += ::createUpgradeCallableReferencesPhase
        }
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)) {
            this += ::NativeAssertionWrapperLowering
        }
        this += loweringsOfTheFirstPhase(languageVersionSettings)
    }
    return createModulePhases(*phases.toTypedArray())
}
