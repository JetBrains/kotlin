/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.phaser.validationCallback
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal abstract class JvmIrValidationPhase(protected val context: JvmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        if (context.config.shouldValidateIr) {
            validate(irModule)
        }
    }

    protected abstract fun validate(irModule: IrModuleFragment)
}

@PhaseDescription(
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)
internal class JvmIrValidationBeforeLoweringPhase(context: JvmBackendContext) : JvmIrValidationPhase(context) {
    override fun validate(irModule: IrModuleFragment) {
        validationCallback(context, irModule, checkProperties = true)
    }
}

private fun checkAllFileLevelDeclarationsAreClasses(module: IrModuleFragment) {
    assert(module.files.all { irFile ->
        irFile.declarations.all { it is IrClass }
    })
}

@PhaseDescription(
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
)
internal class JvmIrValidationAfterLoweringPhase(context: JvmBackendContext) : JvmIrValidationPhase(context) {
    override fun validate(irModule: IrModuleFragment) {
        validationCallback(context, irModule, checkProperties = true)

        checkAllFileLevelDeclarationsAreClasses(irModule)
        val validator = object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitProperty(declaration: IrProperty) {
                error("No properties should remain at this stage")
            }

            override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
                error("No anonymous initializers should remain at this stage")
            }
        }
        irModule.acceptVoid(validator)
    }
}
