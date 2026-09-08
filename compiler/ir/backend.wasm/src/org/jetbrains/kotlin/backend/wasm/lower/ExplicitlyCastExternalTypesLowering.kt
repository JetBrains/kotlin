/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isExternalType
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.AbstractValueUsageLowering
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Insert casts between external and non-external types
 */
class ExplicitlyCastExternalTypesLowering(val wasmContext: WasmBackendContext) : AbstractValueUsageLowering(wasmContext) {

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol == wasmContext.wasmSymbols.consumeAnyIntoVoid) {
            expression.apply { transformChildrenVoid() }
            return expression
        } else {
            return super.visitCall(expression)
        }
    }

    override fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression {
        val expectedExternal = isExternalType(expectedType)
        val actualExternal = isExternalType(actualType)

        if (expectedExternal != actualExternal) {
            return JsIrBuilder.buildImplicitCast(this, toType = expectedType)
        }

        return this
    }

    override fun useAsVarargElement(element: IrExpression, expression: IrVararg): IrExpression =
        if (isExternalType(element.type))
            element.useAs(irBuiltIns.anyNType)
        else
            super.useAsVarargElement(element, expression)
}
