/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.lower.inline.AvoidLocalFOsInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler

private val avoidLocalFOsInInlineFunctionsLowering = makeIrModulePhase(
    ::AvoidLocalFOsInInlineFunctionsLowering,
    name = "AvoidLocalFOsInInlineFunctionsLowering",
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
    prerequisite = setOf()
)

private val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = { SyntheticAccessorLowering(it, isExecutedOnFirstPhase = true) },
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase),
)



fun loweringsOfTheFirstPhase(
    @Suppress("UNUSED_PARAMETER") irMangler: IrMangler,
    languageVersionSettings: LanguageVersionSettings
): List<NamedCompilerPhase<PreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> = buildList {
    this += avoidLocalFOsInInlineFunctionsLowering
    if (languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)) {
        this += outerThisSpecialAccessorInInlineFunctionsPhase
        this += syntheticAccessorGenerationPhase
    }
}
