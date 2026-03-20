/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.validation.*
import org.jetbrains.kotlin.ir.validation.checkers.IrNestedOffsetRangeChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrExpressionBodyInFunctionChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrFieldVisibilityChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.InlineFunctionUseSiteChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrCrossFileFieldUsageChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrTypeOperatorRedundancyChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrValueAccessScopeChecker
import org.jetbrains.kotlin.ir.validation.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

abstract class IrValidationPhase<Context : LoweringContext>(val context: Context) : ModuleLoweringPass {
    protected abstract val defaultValidationConfig: IrValidatorConfig

    override fun lower(irModule: IrModuleFragment) {
        val verificationMode = context.configuration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE)
        val phaseName = this.javaClass.simpleName
        validateIr(
            irModule,
            context.irBuiltIns,
            defaultValidationConfig,
            context.diagnosticReporter,
            verificationMode,
            phaseName,
        )
    }
}

class KlibIrValidationBeforeLoweringPhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            .withCheckers(
                IrValueAccessScopeChecker,
                IrExpressionBodyInFunctionChecker,
                IrFieldVisibilityChecker,
                IrCrossFileFieldUsageChecker,
            )
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            //.withCheckers(IrTypeParameterScopeChecker) // TODO: Re-enable checking out-of-scope type parameter usages (KT-69305)
            .applyIf(context.configuration.enableIrVisibilityChecks) {
                withCheckers(IrVisibilityChecker.Relaxed)
            }
            .applyIf(context.configuration.enableIrNestedOffsetsChecks) {
                withCheckers(IrNestedOffsetRangeChecker)
            }
            .applyIf(context.configuration.enableIrVarargTypesChecks) {
                withVarargChecks()
            }
}

class IrValidationAfterInliningOnlyPrivateFunctionsPhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            .applyIf(context.configuration.enableIrVisibilityChecks) {
                withCheckers(IrVisibilityChecker.Relaxed)
            }
            .applyIf(context.configuration.enableIrNestedOffsetsChecks) {
                withCheckers(IrNestedOffsetRangeChecker)
            }
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .applyIf(context.configuration.enableIrVarargTypesChecks) {
                withVarargChecks()
            }
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
}

class IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .applyIf(context.configuration.enableIrVisibilityChecks) {
                withCheckers(IrVisibilityChecker.Relaxed, IrCrossFileFieldUsageChecker, IrValueAccessScopeChecker)
            }
            .applyIf(context.configuration.enableIrNestedOffsetsChecks) {
                withCheckers(IrNestedOffsetRangeChecker)
            }
            .applyIf(context.configuration.enableIrVarargTypesChecks) {
                withVarargChecks()
            }
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckers(IrTypeOperatorRedundancyChecker)
}

class IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig()
            .withCheckers(IrTypeOperatorRedundancyChecker)
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
}

open class IrValidationAfterLoweringPhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            .applyIf(context.configuration.enableIrNestedOffsetsChecks) {
                withCheckers(IrNestedOffsetRangeChecker)
            }
}
