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
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrClassSuperTypesChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrExpressionBodyInFunctionChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrFieldVisibilityChecker
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrPrivateDeclarationOverrideChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.InlineFunctionUseSiteChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrCallTypeArgumentCountChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrCallValueArgumentCountChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrCrossFileFieldUsageChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrTypeOperatorRedundancyChecker
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrValueAccessScopeChecker
import org.jetbrains.kotlin.ir.validation.checkers.symbol.IrVisibilityChecker
import org.jetbrains.kotlin.ir.validation.checkers.type.IrTypeParameterScopeChecker

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

class IrValidationAfterInliningPrivateFunctionsKlibPhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            // Only checks specific to inlining.
            .withBasicChecks()
            .withCheckers(IrVisibilityChecker.Relaxed)
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

class IrValidationAfterInliningAllFunctionsKlibFirstStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            // Only checks specific to inlining.
            //.withBasicChecks() // Don't run basic checks as unbound symbols may be present after inlining from other Klibs.
            .withCheckers(IrTypeOperatorRedundancyChecker, IrTypeParameterScopeChecker)
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

class IrValidationAfterInliningAllFunctionsKlibSecondStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            // All feasible checks.
            .withBasicChecks()
            .withCheckers(
                IrCallValueArgumentCountChecker,
                IrCrossFileFieldUsageChecker,
                IrValueAccessScopeChecker,
                //IrTypeParameterScopeChecker,
                IrVisibilityChecker.Relaxed,
                //IrCallTypeArgumentCountChecker, // KT-80065
                IrFieldVisibilityChecker,
                IrExpressionBodyInFunctionChecker,
                IrTypeOperatorRedundancyChecker,
                IrClassSuperTypesChecker,
            )
            .withVarargChecks()
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

class IrValidationBeforeLoweringsKlibSecondStagePhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true, checkUnboundSymbols = true)
            // All feasible checks.
            .withBasicChecks()
            .withCheckers(
                IrCallValueArgumentCountChecker,
                IrCrossFileFieldUsageChecker,
                IrValueAccessScopeChecker,
                //IrTypeParameterScopeChecker,
                IrVisibilityChecker.Relaxed,
                //IrCallTypeArgumentCountChecker, // KT-80065
                IrFieldVisibilityChecker,
                IrExpressionBodyInFunctionChecker,
                IrClassSuperTypesChecker,
            )
            .withVarargChecks()
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

open class IrValidationAfterLoweringsSecondStagePhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            // Only basic checks.
            .withBasicChecks()
            .withoutCheckersByName(context.configuration.disableIrCheckers)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
}
