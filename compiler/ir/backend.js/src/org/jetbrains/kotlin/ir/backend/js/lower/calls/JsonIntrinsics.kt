/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName


class JsonIntrinsics(val context: JsIrBackendContext) : CallsTransformer {

    override fun transformFunctionAccess(call: IrFunctionAccessExpression): IrExpression {

        fun generateMemberAccess(): IrExpression {
            val obj = call.dispatchReceiver!!
            val propertyName = call.getValueArgument(0)!!
            return IrDynamicOperatorExpressionImpl(
                call.startOffset,
                call.endOffset,
                context.irBuiltIns.anyNType,
                operator = IrDynamicOperator.ARRAY_ACCESS
            ).also {
                it.receiver = obj
                it.arguments.add(propertyName)
            }
        }

        when (call.symbol.owner.fqNameWhenAvailable) {
            FqName("kotlin.js.Json.get") ->
                return generateMemberAccess()

            FqName("kotlin.js.Json.set") -> {
                val value = call.getValueArgument(1)!!

                return IrDynamicOperatorExpressionImpl(call.startOffset, call.endOffset, call.type, IrDynamicOperator.EQ).also {
                    it.receiver = generateMemberAccess()
                    it.arguments.add(value)
                }

            }
        }
        return call
    }
}
