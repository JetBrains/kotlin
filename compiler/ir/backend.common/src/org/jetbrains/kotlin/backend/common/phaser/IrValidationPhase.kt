/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

abstract class IrValidationPhase<Context : LoweringContext>(val context: Context) : ModuleLoweringPass {
    protected abstract val defaultValidationConfig: IrValidatorConfig

    final override fun lower(irModule: IrModuleFragment) {
        val verificationMode = context.configuration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE)
        val phaseName = this.javaClass.simpleName
        validateIr(context.configuration.messageCollector, verificationMode) {
            performBasicIrValidation(
                irModule,
                context.irBuiltIns,
                phaseName,
                defaultValidationConfig,
            )
            additionalValidation(irModule, phaseName)
        }
    }

    protected open fun IrValidationContext.additionalValidation(irModule: IrModuleFragment, phaseName: String) {}
}

@PhaseDescription(name = "ValidateIrBeforeLowering")
open class IrValidationBeforeLoweringPhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(
            checkTypes = false, // TODO: Re-enable checking types (KT-68663)
            checkValueScopes = false,
            checkTypeParameterScopes = false, // TODO: Re-enable checking out-of-scope type parameter usages (KT-69305)
            checkCrossFileFieldUsage = false,
            checkAllKotlinFieldsArePrivate = false,
            checkVisibilities = false,
            checkVarargTypes = false,
        )
}

@PhaseDescription(name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase")
class IrValidationAfterInliningOnlyPrivateFunctionsPhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(
            checkTypes = false, // TODO: Re-enable checking types (KT-68663)
            checkVisibilities = false, // TODO: Enable checking visibilities (KT-69516)
            checkInlineFunctionUseSites = checkInlineFunctionCallSites,
            checkVarargTypes = false,
        )
}

class IrValidationAfterInliningAllFunctionsPhase<Context : LoweringContext>(
    context: Context,
    private val checkInlineFunctionCallSites: InlineFunctionUseSiteChecker? = null
) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig(
            checkTypes = false, // TODO: Re-enable checking types (KT-68663)
            checkValueScopes = false,
            checkCrossFileFieldUsage = false,
            checkTypeParameterScopes = false,
            checkVisibilities = true,
            checkInlineFunctionUseSites = checkInlineFunctionCallSites,
            checkVarargTypes = false,
        )
}

open class IrValidationAfterLoweringPhase<Context : LoweringContext>(context: Context) : IrValidationPhase<Context>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = IrValidatorConfig()
}
