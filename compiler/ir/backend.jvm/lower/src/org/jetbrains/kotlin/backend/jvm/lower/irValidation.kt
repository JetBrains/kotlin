/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationBeforeLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@PhaseDescription(name = "JvmValidateIrBeforeLowering")
internal class JvmK1IrValidationBeforeLoweringPhase(
    context: JvmBackendContext,
) : IrValidationBeforeLoweringPhase<JvmBackendContext>(context) {
    override fun lower(irModule: IrModuleFragment) {
        if (context.config.useFir) {
            // In K2, the validation is performed in FIR2IR, right before lowerings, so no need to repeat it here.
            return
        }
        super.lower(irModule)
    }
}

@PhaseDescription(name = "JvmValidateIrAfterLowering")
internal class JvmIrValidationAfterLoweringPhase(
    context: JvmBackendContext,
) : IrValidationAfterLoweringPhase<JvmBackendContext>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = super.defaultValidationConfig
            .withCheckers(NoTopLevelDeclarationsChecker, NoPropertiesChecker, NoAnonymousInitializersChecker)
}

private object NoTopLevelDeclarationsChecker : IrElementChecker<IrFile>(IrFile::class) {
    override fun check(element: IrFile, context: CheckerContext) {
        for (declaration in element.declarations) {
            if (declaration !is IrClass) {
                context.error(declaration, "The only top-level declarations left should be IrClasses")
            }
        }
    }
}

private object NoPropertiesChecker : IrElementChecker<IrProperty>(IrProperty::class) {
    override fun check(element: IrProperty, context: CheckerContext) {
        context.error(element, "No properties should remain at this stage")
    }
}

private object NoAnonymousInitializersChecker : IrElementChecker<IrAnonymousInitializer>(IrAnonymousInitializer::class) {
    override fun check(element: IrAnonymousInitializer, context: CheckerContext) {
        context.error(element, "No anonymous initializers should remain at this stage")
    }
}
