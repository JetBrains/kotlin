/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.performBasicIrValidation
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationBeforeLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.reportIrValidationError
import org.jetbrains.kotlin.backend.common.throwValidationErrorIfNeeded
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@PhaseDescription(
    name = "JvmValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)
internal class JvmIrValidationBeforeLoweringPhase(
    context: JvmBackendContext
) : IrValidationBeforeLoweringPhase<JvmBackendContext>(context) {
    override fun validate(irModule: IrModuleFragment, mode: IrVerificationMode, phaseName: String) {
        performBasicIrValidation(context, irModule, mode, phaseName, checkProperties = true)
    }
}

@PhaseDescription(
    name = "JvmValidateIrAfterLowering",
    description = "Validate IR after lowering"
)
internal class JvmIrValidationAfterLoweringPhase(
    context: JvmBackendContext
) : IrValidationAfterLoweringPhase<JvmBackendContext>(context) {
    override fun validate(irModule: IrModuleFragment, mode: IrVerificationMode, phaseName: String) {
        performBasicIrValidation(context, irModule, mode, phaseName, checkProperties = true)

        for (file in irModule.files) {
            for (declaration in file.declarations) {
                if (declaration !is IrClass) {
                    context.reportIrValidationError(
                        mode,
                        file,
                        declaration,
                        "The only top-level declarations left should be IrClasses",
                        phaseName,
                    )
                }
            }
        }

        val validator = object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitProperty(declaration: IrProperty) {
                context.reportIrValidationError(
                    mode,
                    declaration.fileOrNull,
                    declaration,
                    "No properties should remain at this stage",
                    phaseName,
                )
            }

            override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
                context.reportIrValidationError(
                    mode,
                    declaration.fileOrNull,
                    declaration,
                    "No anonymous initializers should remain at this stage",
                    phaseName,
                )
            }
        }
        irModule.acceptVoid(validator)
        context.throwValidationErrorIfNeeded(mode)
    }
}
