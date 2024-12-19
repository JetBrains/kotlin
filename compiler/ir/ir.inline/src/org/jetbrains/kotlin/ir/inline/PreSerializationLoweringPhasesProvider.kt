/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler

abstract class PreSerializationLoweringPhasesProvider<Context : LoweringContext> {

    protected open val klibAssertionWrapperLowering: ((Context) -> FileLoweringPass)?
        get() = null

    protected open val jsCodeOutliningLowering: ((Context) -> FileLoweringPass)?
        get() = null

    protected open val allowExternalInlineFunctions: Boolean
        get() = false

    protected abstract val irMangler: IrMangler

    @Suppress("unused") // TODO: Will be used when KT-71415 is fixed
    private fun privateInlineFunctionResolver(context: Context): InlineFunctionResolver {
        return PreSerializationPrivateInlineFunctionResolver(
            context = context,
            allowExternalInlining = allowExternalInlineFunctions,
        )
    }

    @Suppress("unused") // TODO: Will be used when KT-71415 is fixed
    private fun nonPrivateInlineFunctionResolver(context: Context): InlineFunctionResolver {
        return PreSerializationNonPrivateInlineFunctionResolver(
            context = context,
            allowExternalInlining = allowExternalInlineFunctions,
            irMangler = irMangler,
        )
    }

    // TODO: The commented out lowerings must be copied here from the second compilation stage in scope of KT-71415
    fun lowerings(configuration: CompilerConfiguration): SameTypeNamedCompilerPhase<Context, IrModuleFragment> =
        SameTypeNamedCompilerPhase(
            name = "PreSerializationLowerings",
            actions = DEFAULT_IR_ACTIONS,
            nlevels = 1,
            lower = buildModuleLoweringsPhase(
                ::IrValidationBeforeLoweringPhase,
            ) then performByIrFile(
                name = "PrepareForFunctionInlining",
                createFilePhases(
                    klibAssertionWrapperLowering, // Only on Native
                    jsCodeOutliningLowering, // Only on JS
//                  ::NullableFieldsForLateinitCreationLowering,
//                  ::NullableFieldsDeclarationLowering,
//                  ::LateinitUsageLowering,
//                  ::SharedVariablesLowering,
//                  ::OuterThisInInlineFunctionsSpecialAccessorLowering,
//                  ::LocalClassesInInlineLambdasLowering,
//                  { CommonInlineCallableReferenceToLambdaPhase(it, inlineFunctionResolver(context, InlineMode.ALL_INLINE_FUNCTIONS)) },
//                  ::ArrayConstructorLowering,
//                  ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
//                  { _ -> CacheInlineFunctionsBeforeInlining(cacheOnlyPrivateFunctions = true) },
//                  { FunctionInlining(it, inlineFunctionResolver(context, InlineMode.PRIVATE_INLINE_FUNCTIONS), produceOuterThisFields = false) },
//                  ::SyntheticAccessorLowering,
//                  { _ -> CacheInlineFunctionsBeforeInlining(cacheOnlyPrivateFunctions = false) },
                ),
            ) then buildModuleLoweringsPhase(
//              validateIrAfterInliningOnlyPrivateFunctions,
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
