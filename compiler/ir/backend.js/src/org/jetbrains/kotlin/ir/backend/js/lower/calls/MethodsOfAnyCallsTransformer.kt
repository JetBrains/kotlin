/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

class MethodsOfAnyCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val nameToTransformer: Map<Name, (IrFunctionAccessExpression) -> IrExpression>

    init {
        nameToTransformer = hashMapOf()
        nameToTransformer.run {
            put(OperatorNameConventions.TO_STRING) { call ->
                if (shouldReplaceToStringWithRuntimeCall(call)) {
                    if ((call as IrCall).isSuperToAny()) {
                        irCall(call, intrinsics.jsAnyToString)
                    } else {
                        irCall(call, intrinsics.jsToString)
                    }
                } else {
                    call
                }
            }

            put(OperatorNameConventions.HASH_CODE) { call ->
                if (call.symbol.owner.isFakeOverriddenFromAny()) {
                    if ((call as IrCall).isSuperToAny()) {
                        irCall(call, intrinsics.jsGetObjectHashCode)
                    } else {
                        irCall(call, intrinsics.jsHashCode)
                    }
                } else {
                    call
                }
            }
        }
    }


    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val symbol = call.symbol
        nameToTransformer[symbol.owner.name]?.let {
            return it(call)
        }

        return call
    }

    private fun shouldReplaceToStringWithRuntimeCall(call: IrFunctionAccessExpression): Boolean {
        val function = call.symbol.owner
        if (function.name != OperatorNameConventions.TO_STRING || !function.hasShape(dispatchReceiver = true, regularParameters = 0))
            return false

        if (call is IrCall) {
            val superQualifierSymbol = call.superQualifierSymbol
            if (superQualifierSymbol != null &&
                !superQualifierSymbol.owner.isInterface &&
                superQualifierSymbol != intrinsics.anyClassSymbol) {
                return false
            }
        }

        val receiverParameterType = function.dispatchReceiverParameter?.type ?: return false

        return receiverParameterType.run {
            isArray() || isAny() || this is IrDynamicType || isString()
        }
    }
}
