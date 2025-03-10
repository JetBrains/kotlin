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

val nativeLoweringsOfTheFirstPhase: List<NamedCompilerPhase<PreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> =
    listOf(upgradeCallableReferencesPhase, assertionWrapperPhase) + loweringsOfTheFirstPhase(KonanManglerIr)
