/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.FrontendSymbols
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.inline.AvoidLocalFOsInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningOnlyPrivateFunctionsPhase
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

private val lateinitPhase = makeIrModulePhase(
    ::LateinitLowering,
    name = "LateinitLowering",
)

private val sharedVariablesLoweringPhase = makeIrModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    prerequisite = setOf(lateinitPhase)
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
)

private val checkInlineCallCyclesPhase = makeIrModulePhase(
    ::InlineCallCycleCheckerLowering,
    name = "InlineCallCycleChecker",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = makeIrModulePhase(
    { context: LoweringContext ->
        FunctionInlining(
            context,
            PreSerializationPrivateInlineFunctionResolver(context),
        )
    },
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(arrayConstructorPhase, checkInlineCallCyclesPhase),
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase)
)

private val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = { SyntheticAccessorLowering(it, isExecutedOnFirstPhase = true) },
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase, outerThisSpecialAccessorInInlineFunctionsPhase),
)

private val validateIrAfterInliningOnlyPrivateFunctions = makeIrModulePhase(
    { context: LoweringContext ->
        IrValidationAfterInliningOnlyPrivateFunctionsPhase(
            context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                // Call sites of only non-private functions are allowed at this stage.
                !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
            }
        )
    },
    name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase",
)

private val checkInlineDeclarationsAfterInliningOnlyPrivateFunctions = makeIrModulePhase(
    lowering = ::InlineDeclarationCheckerLowering,
    name = "InlineDeclarationCheckerAfterInliningOnlyPrivateFunctionsPhase",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase),
)

private fun inlineAllFunctionsPhase(irMangler: IrMangler) = makeIrModulePhase(
    { context: LoweringContext ->
        FunctionInlining(
            context,
            PreSerializationNonPrivateInlineFunctionResolver(context, irMangler),
        )
    },
    name = "InlineAllFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

private val inlineFunctionSerializationPreProcessing = makeIrModulePhase(
    lowering = ::InlineFunctionSerializationPreProcessing,
    name = "InlineFunctionSerializationPreProcessing",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase, /*inlineAllFunctionsPhase*/),
)

private fun validateIrAfterInliningAllFunctionsPhase(irMangler: IrMangler) = makeIrModulePhase(
    { context: LoweringContext ->
        val resolver = PreSerializationNonPrivateInlineFunctionResolver(context, irMangler)
        IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase(
            context,
            checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val actualCallee = resolver.getFunctionDeclarationToInline(inlineFunctionUseSite)
                when {
                    actualCallee?.body == null -> true // does not have a body <=> should not be inlined
                    // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
                    FrontendSymbols.isTypeOfIntrinsic(actualCallee.symbol) -> true
                    else -> false // forbidden
                }
            }
        )
    },
    name = "IrValidationAfterInliningAllFunctionsPhase",
)

fun loweringsOfTheFirstPhase(
    irMangler: IrMangler,
    languageVersionSettings: LanguageVersionSettings
): List<NamedCompilerPhase<PreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> = buildList {
    this += avoidLocalFOsInInlineFunctionsLowering
    if (languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)) {
        this += lateinitPhase
        this += sharedVariablesLoweringPhase
        this += localClassesInInlineLambdasPhase
        this += arrayConstructorPhase
        this += checkInlineCallCyclesPhase
        this += inlineOnlyPrivateFunctionsPhase
        this += checkInlineDeclarationsAfterInliningOnlyPrivateFunctions
        this += outerThisSpecialAccessorInInlineFunctionsPhase
        this += syntheticAccessorGenerationPhase
        this += validateIrAfterInliningOnlyPrivateFunctions
        this += inlineAllFunctionsPhase(irMangler)
        this += inlineFunctionSerializationPreProcessing
        this += validateIrAfterInliningAllFunctionsPhase(irMangler)
    }
}
