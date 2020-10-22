/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.AbstractValueUsageLowering
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNotNull

/**
 * Replace null constants of type Nothing? with null constants of a concrete class types.
 *
 * Wasm GC doesn't have a nullref type anymore.
 */
class WasmNullCoercingLowering(context: JsCommonBackendContext) : AbstractValueUsageLowering(context) {
    override fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression =
        if (actualType.makeNotNull().isNothing() && actualType.isNullable() && !expectedType.makeNotNull().isNothing())
            JsIrBuilder.buildComposite(
                type,
                listOf(this, IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expectedType, IrConstKind.Null, null))
            )
        else
            this
}