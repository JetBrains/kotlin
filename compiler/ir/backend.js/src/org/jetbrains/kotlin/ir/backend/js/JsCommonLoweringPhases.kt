/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.Symbols.Companion.isTypeOfIntrinsic
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.WrapInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsPhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningOnlyPrivateFunctionsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.lower.JsCodeOutliningLowering
import org.jetbrains.kotlin.ir.backend.js.lower.JsInlineCallableReferenceToLambdaPhase
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.inline.*

private val jsCodeOutliningPhase = makeIrModulePhase(
    { context: JsPreSerializationLoweringContext -> JsCodeOutliningLowering(context, context.intrinsics, context.dynamicType) },
    name = "JsCodeOutliningLowering",
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

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
)

private val inlineCallableReferenceToLambdaPhase = makeIrModulePhase(
    ::JsInlineCallableReferenceToLambdaPhase,
    name = "JsInlineCallableReferenceToLambdaPhase",
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
            PreSerializationPrivateInlineFunctionResolver(context, allowExternalInlining = true),
            produceOuterThisFields = false,
        )
    },
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

private val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = ::SyntheticAccessorLowering,
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase),
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
                    else -> !inlineFunction.isConsideredAsPrivateForInlining()
                }
            }
        )
    },
    name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase",
)

/**
 * The second phase of inlining (inline all functions).
 */
@Suppress("Unused") // KT-72777
private val inlineAllFunctionsPhase = makeIrModulePhase(
    { context: LoweringContext ->
        FunctionInlining(
            context,
            PreSerializationNonPrivateInlineFunctionResolver(context, allowExternalInlining = true, irMangler = JsManglerIr),
            produceOuterThisFields = false,
        )
    },
    name = "InlineAllFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

@Suppress("Unused") // KT-72777
private val validateIrAfterInliningAllFunctions = makeIrModulePhase(
    { context: LoweringContext ->
        IrValidationAfterInliningAllFunctionsPhase(
            context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-69457:
                    inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                    // TODO: remove this condition after the fix of KT-70361:
                    isTypeOfIntrinsic(inlineFunction.symbol) -> true // temporarily permitted

                    else -> false // forbidden
                }
            }
        )
    },
    name = "IrValidationAfterInliningAllFunctionsPhase",
)

internal val loweringsOfTheFirstPhase: List<SimpleNamedCompilerPhase<JsPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> = listOfNotNull(
    jsCodeOutliningPhase,
    lateinitPhase,
    sharedVariablesLoweringPhase,
    outerThisSpecialAccessorInInlineFunctionsPhase,
    localClassesInInlineLambdasPhase,
    inlineCallableReferenceToLambdaPhase,
    arrayConstructorPhase,
    wrapInlineDeclarationsWithReifiedTypeParametersLowering,
    inlineOnlyPrivateFunctionsPhase,
    syntheticAccessorGenerationPhase,
    validateIrAfterInliningOnlyPrivateFunctions,
    // TODO KT-72777
//    inlineAllFunctionsPhase,
//    validateIrAfterInliningAllFunctions,
)
