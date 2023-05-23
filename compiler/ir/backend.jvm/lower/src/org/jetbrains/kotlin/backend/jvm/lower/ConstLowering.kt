/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.InlineConstTransformer
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.interpreter.transformer.reportOnIr
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val constPhase1 = makeIrFilePhase(
    ::ConstLowering,
    name = "Const1",
    description = "Substitute calls to const properties with constant values"
)

internal val constPhase2 = makeIrFilePhase(
    ::ConstLowering,
    name = "Const2",
    description = "Substitute calls to const properties with constant values"
)

class ConstLowering(val context: JvmBackendContext) : FileLoweringPass {
    val inlineConstTracker =
        context.state.configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER]

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid(JvmInlineConstTransformer(irFile, inlineConstTracker))
}

private class JvmInlineConstTransformer(val irFile: IrFile, val inlineConstTracker: InlineConstTracker?) : InlineConstTransformer() {
    override val IrField.constantInitializer get() = constantValue()
    override fun reportInlineConst(field: IrField, value: IrConst<*>) {
        inlineConstTracker?.reportOnIr(irFile, field, value)
    }
}