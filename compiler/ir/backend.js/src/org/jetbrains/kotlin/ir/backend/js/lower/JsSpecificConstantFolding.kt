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
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.types.impl.IrUnionType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isNullableNothing
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.isNullable

class JsSpecificConstantFoldingLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = JsSpecificConstantFoldingLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class JsSpecificConstantFoldingLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    private val unitType = context.irBuiltIns.unitType
    private val unitValue get() = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, unitType, context.irBuiltIns.unitClass)

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        val value = expression.value
        return if (expression.origin == JsStatementOrigins.DEFAULT_ARGUMENT_RESOLUTION && value is IrGetValue && expression.symbol == value.symbol) {
            unitValue
        } else {
            super.visitSetValue(expression)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val result = when {
            expression.extensionReceiver != null -> expression
            expression.dispatchReceiver == null && expression.valueArgumentsCount == 1 -> tryFoldingBuiltinUnaryOps(expression)
            expression.dispatchReceiver == null && expression.valueArgumentsCount == 2 -> tryFoldingBuiltinBinaryOps(expression)
            else -> expression
        }

        return super.visitExpression(result)
    }

    private fun tryFoldingBuiltinUnaryOps(call: IrCall): IrExpression {
        val value = call.getValueArgument(0) ?: return call

        if (call.symbol == context.intrinsics.jsIsUndefined && !value.isNullable()) {
            return IrConstImpl.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
        }

        return call
    }

    private fun tryFoldingBuiltinBinaryOps(call: IrCall): IrExpression {
        val lhs = call.getValueArgument(0) ?: return call
        val rhs = call.getValueArgument(1) ?: return call

        if (
            call.origin == IrStatementOrigin.EQEQ ||
            call.origin == IrStatementOrigin.EQEQEQ ||
            call.origin == IrStatementOrigin.SYNTHETIC_NOT_AUTOBOXED_CHECK
        ) {
            if (lhs.isConstNullable() && rhs.isConstNullable()) {
                return IrConstImpl.constTrue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (!lhs.isNullable() && rhs.isConstNullable()) {
                return IrConstImpl.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (!rhs.isNullable() && lhs.isConstNullable()) {
                return IrConstImpl.constFalse(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (lhs.isVoid() && rhs.isConstNullable()) {
                return IrConstImpl.constTrue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            } else if (rhs.isVoid() && lhs.isConstNullable()) {
                return IrConstImpl.constTrue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType)
            }
        }

        return call
    }

    private fun IrExpression.isConstNullable(): Boolean {
        return isNullConst() || this is IrGetField && symbol == context.intrinsics.void.owner.backingField?.symbol
    }

    private fun IrExpression.isNullable(): Boolean {
        return when (this) {
            is IrGetValue -> symbol.owner.type.isNullable()
            else -> type.isNullable()
        }
    }

    private fun IrExpression.isVoid(): Boolean {
        return when (this) {
            is IrGetValue -> symbol.owner.type.isVoid()
            else -> type.isVoid()
        }
    }

    private fun IrType.isVoid(): Boolean {
        return when (this) {
            is IrUnionType -> types.all { it.isVoid() }
            else -> isNullableNothing()
        }
    }

}
