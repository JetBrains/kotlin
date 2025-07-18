/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.declaration.IrFieldVisibilityChecker
import org.jetbrains.kotlin.backend.common.checkers.expression.IrCrossFileFieldUsageChecker
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationBeforeLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.*

@PhaseDescription(name = "JvmValidateIrBeforeLowering")
internal class JvmIrValidationBeforeLoweringPhase(
    context: JvmBackendContext,
) : IrValidationBeforeLoweringPhase<JvmBackendContext>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = super.defaultValidationConfig
            .withCommonCheckers(checkProperties = true)
            .apply {
                copy(
                    elementCheckers = elementCheckers
                            - setOf(IrCrossFileFieldUsageChecker, IrFieldVisibilityChecker, IrCrossFileFieldUsageChecker)
                )
            }
}

@PhaseDescription(name = "JvmValidateIrAfterLowering")
internal class JvmIrValidationAfterLoweringPhase(
    context: JvmBackendContext,
) : IrValidationAfterLoweringPhase<JvmBackendContext>(context) {
    override val defaultValidationConfig: IrValidatorConfig
        get() = super.defaultValidationConfig
            .withCommonCheckers(checkProperties = true)
            .apply {
                copy(
                    elementCheckers = elementCheckers
                            - setOf(IrCrossFileFieldUsageChecker, IrFieldVisibilityChecker, IrCrossFileFieldUsageChecker)
                            + listOf(NoTopLevelDeclarationsChecker, NoPropertiesChecker, NoAnonymousInitializersChecker)
                )
            }
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