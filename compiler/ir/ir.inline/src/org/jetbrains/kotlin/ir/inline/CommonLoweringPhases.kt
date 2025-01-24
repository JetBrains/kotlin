/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.WrapInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningOnlyPrivateFunctionsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference

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

private val inlineCallableReferenceToLambdaPhase = makeIrModulePhase(
    lowering = { context: LoweringContext ->
        CommonInlineCallableReferenceToLambdaPhase(
            context,
            PreSerializationPrivateInlineFunctionResolver(context)
        )
    },
    name = "InlineCallableReferenceToLambdaPhase",
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    prerequisite = setOf(inlineCallableReferenceToLambdaPhase)
)

private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = makeIrModulePhase(
    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "WrapInlineDeclarationsWithReifiedTypeParametersLowering",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = makeIrModulePhase(
    { context: LoweringContext ->
        FunctionInlining(
            context,
            PreSerializationPrivateInlineFunctionResolver(context),
            produceOuterThisFields = false,
        )
    },
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(wrapInlineDeclarationsWithReifiedTypeParametersLowering, arrayConstructorPhase)
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
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-69457:
                    inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                    // Call sites of only non-private functions are allowed at this stage.
                    else -> !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
                }
            }
        )
    },
    name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase",
)

///**
// * The second phase of inlining (inline all functions).
// */
//private val inlineAllFunctionsPhase = makeIrModulePhase(
//    { context: LoweringContext ->
//        FunctionInlining(
//            context,
//            PreSerializationNonPrivateInlineFunctionResolver(context, irMangler = TODO()),
//            produceOuterThisFields = false,
//        )
//    },
//    name = "InlineAllFunctions",
//    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
//)

val loweringsOfTheFirstPhase: List<SimpleNamedCompilerPhase<LoweringContext, IrModuleFragment, IrModuleFragment>> = listOf(
    lateinitPhase,
    sharedVariablesLoweringPhase,
    localClassesInInlineLambdasPhase,
    inlineCallableReferenceToLambdaPhase,
    arrayConstructorPhase,
    wrapInlineDeclarationsWithReifiedTypeParametersLowering,
    inlineOnlyPrivateFunctionsPhase,
    outerThisSpecialAccessorInInlineFunctionsPhase,
    syntheticAccessorGenerationPhase,
    validateIrAfterInliningOnlyPrivateFunctions,
//         TODO KT-72441 add public inlining to this list
//        inlineAllFunctionsPhase,
//        validateIrAfterInliningAllFunctions
)
