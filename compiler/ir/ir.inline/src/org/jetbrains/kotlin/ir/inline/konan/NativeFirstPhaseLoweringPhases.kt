/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline.konan

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.loweringsOfTheFirstPhase
import org.jetbrains.kotlin.ir.util.isReifiedTypeParameter

private val upgradeCallableReferencesPhase = makeIrModulePhase(
    lowering = ::UpgradeCallableReferences,
    name = "UpgradeCallableReferences"
)

private val assertionWrapperPhase = makeIrModulePhase(
    lowering = ::NativeAssertionWrapperLowering,
    name = "AssertionWrapperLowering",
)

private val validateIrAfterInliningAllFunctions = makeIrModulePhase(
    name = "ValidateIrAfterInliningAllFunctions",
    lowering = { context: LoweringContext ->
        IrValidationAfterInliningAllFunctionsPhase(
            context = context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-66734:
                    inlineFunction.isExternal -> true // temporarily permitted

                    // it's fine to have typeOf<T> with reified T, it would be correctly handled by inliner on inlining to next use-sites.
                    // maybe it should be replaced by separate node to avoid this special case and simplify detection code - KT-70360
                    Symbols.isTypeOfIntrinsic(inlineFunction.symbol) && inlineFunctionUseSite.typeArguments[0]?.isReifiedTypeParameter == true -> true

                    else -> false // forbidden
                }
            }
        )
    }
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
    this += validateIrAfterInliningAllFunctions
}
