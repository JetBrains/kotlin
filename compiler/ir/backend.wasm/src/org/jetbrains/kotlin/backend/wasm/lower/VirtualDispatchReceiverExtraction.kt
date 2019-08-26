/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// During Wasm codegen, dispatch receiver can be used multiple times.
// Move it to variable if it could have side effects.
class VirtualDispatchReceiverExtraction(val context: CommonBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val function = expression.symbol.owner
                val receiver = expression.dispatchReceiver
                if (receiver == null || function !is IrSimpleFunction || !function.isOverridable)
                    return expression
                // TODO: Are there other simple receivers without side effect?
                if (receiver is IrGetValue)
                    return expression
                val tmp = JsIrBuilder.buildVar(receiver.type, irFunction, initializer = receiver)
                expression.dispatchReceiver = JsIrBuilder.buildGetValue(tmp.symbol)
                return JsIrBuilder.buildBlock(expression.type, listOf(tmp, expression))
            }
        })
    }
}