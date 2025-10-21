/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.inline.AvoidLocalFOsInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningOnlyPrivateFunctionsPhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

private fun createInlineOnlyPrivateFunctionsPhase(context: LoweringContext): FunctionInlining {
    return FunctionInlining(
        context,
        PreSerializationPrivateInlineFunctionResolver(context),
    )
}

private fun createSyntheticAccessorGeneration(context: LoweringContext): SyntheticAccessorLowering {
    return SyntheticAccessorLowering(context, isExecutedOnFirstPhase = true)
}

private fun createValidateIrAfterInliningOnlyPrivateFunctions(context: LoweringContext): IrValidationAfterInliningOnlyPrivateFunctionsPhase<LoweringContext> {
    return IrValidationAfterInliningOnlyPrivateFunctionsPhase(
        context,
        checkInlineFunctionCallSites = { inlineFunctionUseSite ->
            // Call sites of only non-private functions are allowed at this stage.
            !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
        }
    )
}

fun loweringsOfTheFirstPhase(
    irMangler: IrMangler,
    languageVersionSettings: LanguageVersionSettings
): List<(PreSerializationLoweringContext) -> ModuleLoweringPass> {
    val inlineIntraModule = languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)
    val inlineCrossModuleFunctions =
        languageVersionSettings.supportsFeature(LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization)

    fun createInlineAllFunctionsPhase(context: LoweringContext): FunctionInlining {
        return FunctionInlining(
            context,
            PreSerializationNonPrivateInlineFunctionResolver(context, irMangler, inlineCrossModuleFunctions),
        )
    }

    fun createInlineFunctionSerializationPreProcessing(context: LoweringContext): InlineFunctionSerializationPreProcessing {
        // Run the cross-module inliner against pre-processed functions (and only pre-processed functions) if cross-module
        // inlining is not enabled in the main IR tree.
        val inliner: FunctionInlining? = runUnless(inlineCrossModuleFunctions) {
            FunctionInlining(
                context,
                PreSerializationNonPrivateInlineFunctionResolver(context, irMangler, inlineCrossModuleFunctions = true),
            )
        }

        return InlineFunctionSerializationPreProcessing(crossModuleFunctionInliner = inliner)
    }

    fun createValidateIrAfterInliningAllFunctionsPhase(context: LoweringContext): IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase<LoweringContext> {
        val resolver = PreSerializationNonPrivateInlineFunctionResolver(context, irMangler, inlineCrossModuleFunctions)
        return IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase(
            context,
            checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val actualCallee = resolver.getFunctionDeclarationToInline(inlineFunctionUseSite)
                when {
                    actualCallee?.body == null -> true // does not have a body <=> should not be inlined
                    // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
                    PreSerializationSymbols.isTypeOfIntrinsic(actualCallee.symbol) -> true
                    else -> false // forbidden
                }
            }
        )
    }

    return buildList {
        this += ::AvoidLocalFOsInInlineFunctionsLowering
        if (inlineIntraModule) {
            this += ::LateinitLowering
            this += ::SharedVariablesLowering
            this += ::LocalClassesInInlineLambdasLowering
            this += ::ArrayConstructorLowering
            this += ::InlineCallCycleCheckerLowering
            this += ::createInlineOnlyPrivateFunctionsPhase
            this += ::InlineDeclarationCheckerLowering
            this += ::OuterThisInInlineFunctionsSpecialAccessorLowering
            this += ::createSyntheticAccessorGeneration
            this += ::createValidateIrAfterInliningOnlyPrivateFunctions
            this += ::createInlineAllFunctionsPhase
            this += ::createInlineFunctionSerializationPreProcessing
            this += ::createValidateIrAfterInliningAllFunctionsPhase
        }
    }
}
