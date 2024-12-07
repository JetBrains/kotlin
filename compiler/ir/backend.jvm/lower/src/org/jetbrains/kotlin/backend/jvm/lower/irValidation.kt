/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.IrValidationContext
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationBeforeLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@PhaseDescription(name = "JvmValidateIrBeforeLowering")
internal class JvmIrValidationBeforeLoweringPhase(
    context: JvmBackendContext
) : IrValidationBeforeLoweringPhase<JvmBackendContext>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = super.defaultValidationConfig.copy(
            checkProperties = true,
            checkCrossFileFieldUsage = false,
            checkAllKotlinFieldsArePrivate = false,
        )
}

@PhaseDescription(name = "JvmValidateIrAfterLowering")
internal class JvmIrValidationAfterLoweringPhase(
    context: JvmBackendContext
) : IrValidationAfterLoweringPhase<JvmBackendContext>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = super.defaultValidationConfig.copy(
            checkProperties = true,
            checkCrossFileFieldUsage = false,
            checkAllKotlinFieldsArePrivate = false,
        )

    override fun IrValidationContext.additionalValidation(irModule: IrModuleFragment, phaseName: String) {
        for (file in irModule.files) {
            for (declaration in file.declarations) {
                if (declaration !is IrClass) {
                    reportIrValidationError(
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
                reportIrValidationError(
                    declaration.fileOrNull,
                    declaration,
                    "No properties should remain at this stage",
                    phaseName,
                )
            }

            override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
                reportIrValidationError(
                    declaration.fileOrNull,
                    declaration,
                    "No anonymous initializers should remain at this stage",
                    phaseName,
                )
            }
        }
        irModule.acceptVoid(validator)
    }
}
