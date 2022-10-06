/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class JsSpecificConstantFoldingLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = JsSpecificConstantFoldingLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class JsSpecificConstantFoldingLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        return when {
            expression.extensionReceiver != null -> expression
            expression.dispatchReceiver == null && expression.valueArgumentsCount == 2 -> tryFoldingBuiltinBinaryOps(expression)
            else -> expression
        }
    }

    private fun tryFoldingBuiltinBinaryOps(call: IrCall): IrExpression {
        val lhs = call.getValueArgument(0) ?: return call
        val rhs = call.getValueArgument(1) ?: return call

        if (call.origin == IrStatementOrigin.EQEQ) {
            if (lhs.isNullable() && rhs.isNullable()) {
                return IrConstImpl.constTrue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (lhs is IrConst<*> && rhs.isNullable()) {
                return IrConstImpl.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (rhs is IrConst<*> && lhs.isNullable()) {
                return IrConstImpl.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            }
        }

        return call
    }

    private fun IrExpression.isNullable(): Boolean {
        return isNullConst() || this is IrGetField && symbol == context.intrinsics.void.owner.backingField?.symbol
    }
}
