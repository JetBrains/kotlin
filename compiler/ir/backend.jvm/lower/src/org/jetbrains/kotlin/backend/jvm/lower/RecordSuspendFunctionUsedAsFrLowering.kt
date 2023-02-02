/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val recordSuspendFunctionUsedAsFrLoweringPhase = makeIrFilePhase(
    ::RecordSuspendFunctionUsedAsFrLowering,
    name = "RecordSuspendFunctionUsedAsFrLowering",
    description = "Record suspend function used as function reference lowering phase"
)

class RecordSuspendFunctionUsedAsFrLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private fun IrFunctionReference.isSuspendFunctionReference(): Boolean = isSuspend &&
            (origin == null || origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || origin == IrStatementOrigin.SUSPEND_CONVERSION)

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (expression.isSuspendFunctionReference()) {
            val owner = expression.symbol.owner
            if (owner.returnType.isNullableAny() && owner.originalFunction.returnType.isUnit()) {
                context.suspendFunctionUsedAsFunctionRef.add(owner)
            }
        }
        return expression
    }
}
