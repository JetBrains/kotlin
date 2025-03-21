/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline.konan

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.loweringsOfTheFirstPhase

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
): List<NamedCompilerPhase<PreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> = buildList {
    // TODO: after the fix of KT-76260 this condition should be simplified to just the check of `IrRichCallableReferencesInKlibs` feature
    val enableRichReferences = languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs) ||
            languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)

    if (enableRichReferences) {
        this += upgradeCallableReferencesPhase
    }
    if (languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)) {
        this += assertionWrapperPhase
    }
    this += loweringsOfTheFirstPhase(KonanManglerIr, languageVersionSettings)
}
