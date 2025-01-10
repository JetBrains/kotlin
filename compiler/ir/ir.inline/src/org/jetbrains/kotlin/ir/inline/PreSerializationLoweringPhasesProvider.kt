/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.LateinitLowering
import org.jetbrains.kotlin.backend.common.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.common.lower.WrapInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.config.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler

abstract class PreSerializationLoweringPhasesProvider<Context : LoweringContext> {

    protected open val klibAssertionWrapperLowering: ((Context) -> FileLoweringPass)?
        get() = null

    protected open val jsCodeOutliningLowering: ((Context) -> FileLoweringPass)?
        get() = null

    protected open val upgradeCallableReferenceLowering: ((Context) -> FileLoweringPass)?
        get() = null

    protected abstract val irMangler: IrMangler

    abstract fun getLowerings(): List<SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>>

    private fun privateInlineFunctionResolver(context: Context): InlineFunctionResolver {
        return PreSerializationPrivateInlineFunctionResolver(
            context = context,
        )
    }

    @Suppress("unused") // TODO: Will be used when KT-71415 is fixed
    private fun nonPrivateInlineFunctionResolver(context: Context): InlineFunctionResolver {
        return PreSerializationNonPrivateInlineFunctionResolver(
            context = context,
            irMangler = irMangler,
        )
    }

    // TODO: The commented out lowerings must be copied here from the second compilation stage in scope of KT-71415
    fun lowerings(): SameTypeNamedCompilerPhase<Context, IrModuleFragment> {
        fun inlineCallableReferenceToLambdaPhase(context: Context) =
            CommonInlineCallableReferenceToLambdaPhase(context, privateInlineFunctionResolver(context))
        fun privateInline(context: Context) =
            FunctionInlining(context, privateInlineFunctionResolver(context), produceOuterThisFields = false)
        fun validateIrAfterInliningOnlyPrivateFunctions(context: Context) = IrValidationAfterInliningOnlyPrivateFunctionsPhase(
            context = context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-69457:
                    inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                    // Call sites of non-private functions are allowed at this stage.
                    else -> !inlineFunction.isConsideredAsPrivateForInlining()
                }
            }
        )

        return SameTypeNamedCompilerPhase(
            name = "PreSerializationLowerings",
            actions = DEFAULT_IR_ACTIONS,
            nlevels = 1,
            lower = buildModuleLoweringsPhase(
                ::IrValidationBeforeLoweringPhase,
            ) then performByIrFile(
                name = "PrepareForFunctionInlining",
                createFilePhases(
                    upgradeCallableReferenceLowering,
                    klibAssertionWrapperLowering, // Only on Native
                    jsCodeOutliningLowering, // Only on JS
                    ::LateinitLowering,
                    ::SharedVariablesLowering,
                    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
                    ::LocalClassesInInlineLambdasLowering,
                    ::inlineCallableReferenceToLambdaPhase,
                    ::ArrayConstructorLowering,
                    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
                    ::privateInline,
                    ::SyntheticAccessorLowering,
                ),
            ) then buildModuleLoweringsPhase(
                ::validateIrAfterInliningOnlyPrivateFunctions,
            ) then performByIrFile(
                name = "FunctionInlining",
                createFilePhases(
//                  { FunctionInlining(it, inlineFunctionResolver(context, InlineMode.ALL_INLINE_FUNCTIONS), produceOuterThisFields = false) },
                ),
            ) then buildModuleLoweringsPhase(
//              validateIrAfterInliningAllFunctions
            )
        )
    }
}