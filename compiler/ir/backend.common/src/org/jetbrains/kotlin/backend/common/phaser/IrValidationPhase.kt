/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrValidationContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.validateIr
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

abstract class IrValidationPhase<Context : CommonBackendContext>(val context: Context) : ModuleLoweringPass {

    final override fun lower(irModule: IrModuleFragment) {
        val verificationMode = context.configuration.get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE)
        validateIr(context.messageCollector, verificationMode) {
            validate(irModule, phaseName = this@IrValidationPhase.javaClass.simpleName)
        }
    }

    protected abstract fun IrValidationContext.validate(irModule: IrModuleFragment, phaseName: String)
}

open class IrValidationBeforeLoweringPhase<Context : CommonBackendContext>(context: Context) : IrValidationPhase<Context>(context) {
    override fun IrValidationContext.validate(irModule: IrModuleFragment, phaseName: String) {
        performBasicIrValidation(
            irModule,
            context.irBuiltIns,
            phaseName,
            checkTypes = false, // TODO: Re-enable checking types (KT-68663)
        )
    }
}

open class IrValidationAfterLoweringPhase<Context : CommonBackendContext>(context: Context) : IrValidationPhase<Context>(context) {
    override fun IrValidationContext.validate(irModule: IrModuleFragment, phaseName: String) {
        performBasicIrValidation(irModule, context.irBuiltIns, phaseName)
    }
}
