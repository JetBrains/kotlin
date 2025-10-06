/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isExternalType
import org.jetbrains.kotlin.backend.wasm.utils.isJsShareable
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.AbstractValueUsageLowering
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull

/**
 * Insert casts between external and non-external types, as well as between shareable and non-shareable external types.
 */
class ExplicitlyCastExternalTypesLowering(private val wasmContext: WasmBackendContext) : AbstractValueUsageLowering(wasmContext) {

    override fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression {
        val expectedExternal = isExternalType(expectedType)
        val actualExternal = isExternalType(actualType)

        val castNeeded = when {
            expectedExternal != actualExternal -> true
            !expectedExternal -> false
            expectedType.isJsShareable() != actualType.isJsShareable() -> true
            else -> false
        }

        if (castNeeded) {
            return JsIrBuilder.buildImplicitCast(this, toType = expectedType)
        }

        return this
    }

    override fun useAsVarargElement(element: IrExpression, expression: IrVararg): IrExpression =
        if (isExternalType(element.type))
            element.useAs(irBuiltIns.anyNType)
        else
            super.useAsVarargElement(element, expression)

    private fun IrType.isJsShareable() = classOrNull?.owner?.isJsShareable(wasmContext.wasmSymbols) ?: false
}
