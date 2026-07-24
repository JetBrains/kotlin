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
import org.jetbrains.kotlin.ir.validation.checkers.type.IrTypeParameterScopeChecker
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
                IrVisibilityChecker.Relaxed,
                IrCrossFileFieldUsageChecker,
            )
            .withVarargChecks()
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            // FIXME(KT-71243): This checker should be added unconditionally, but currently the ExplicitBackingFields feature de-facto allows specifying
            //  non-private visibilities for fields.
            .applyIf(!context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ExplicitBackingFields)) {
                withCheckers(IrFieldVisibilityChecker)
            }
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)

}

class IrValidationAfterInliningOnlyPrivateFunctionsPhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .withCheckers(IrVisibilityChecker.Relaxed)
            .withVarargChecks()
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

class IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            .withCheckers(
                IrVisibilityChecker.Relaxed,
                IrCrossFileFieldUsageChecker,
                IrValueAccessScopeChecker,
                IrTypeOperatorRedundancyChecker,
            )
            //.withTypeChecks() // TODO: Re-enable checking types (KT-68663)
            .withVarargChecks()
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

class IrValidationAfterInliningAllFunctionsOnTheFirstStagePhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null,
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig()
            .withCheckers(IrTypeOperatorRedundancyChecker, IrTypeParameterScopeChecker)
            .withInlineFunctionCallsiteCheck(checkInlineFunctionCallSites)
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}

open class IrValidationAfterLoweringPhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(checkTreeConsistency = true)
            .withBasicChecks()
            .withCheckersByName(context.configuration.additionalIrCheckers, listOf(IrNestedOffsetRangeChecker))
            .withoutCheckersByName(context.configuration.disableIrCheckers)
}
