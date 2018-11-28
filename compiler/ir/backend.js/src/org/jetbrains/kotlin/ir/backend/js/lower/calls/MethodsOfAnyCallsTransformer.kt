/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSuperToAny
import org.jetbrains.kotlin.name.Name


class MethodsOfAnyCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val nameToTransformer: Map<Name, (IrCall) -> IrExpression>

    init {
        nameToTransformer = mutableMapOf()
        nameToTransformer.run {
            put(Name.identifier("toString")) { call ->
                if (shouldReplaceToStringWithRuntimeCall(call)) {
                    if (call.isSuperToAny()) {
                        irCall(call, intrinsics.jsAnyToString, dispatchReceiverAsFirstArgument = true)
                    } else {
                        irCall(call, intrinsics.jsToString, dispatchReceiverAsFirstArgument = true)
                    }
                } else {
                    call
                }
            }

            put(Name.identifier("hashCode")) { call ->
                if (call.symbol.owner.isFakeOverriddenFromAny()) {
                    if (call.isSuperToAny()) {
                        irCall(call, intrinsics.jsGetObjectHashCode, dispatchReceiverAsFirstArgument = true)
                    } else {
                        irCall(call, intrinsics.jsHashCode, dispatchReceiverAsFirstArgument = true)
                    }
                } else {
                    call
                }
            }
        }
    }


    override fun transformCall(call: IrCall): IrExpression {
        val symbol = call.symbol
        nameToTransformer[symbol.owner.name]?.let {
            return it(call)
        }

        return call
    }

    private fun shouldReplaceToStringWithRuntimeCall(call: IrCall): Boolean {
        // TODO: (KOTLIN-CR-2079)
        //  - User defined extension functions Any?.toString() call can be lost during lowering.
        //  - Use direct method call for dynamic types???
        //  - Define Any?.toString() in runtime library and stop intrinsicifying extensions

        if (call.valueArgumentsCount > 0)
            return false

        val receiverParameterType = with(call.symbol.owner) {
            dispatchReceiverParameter ?: extensionReceiverParameter
        }?.type ?: return false

        return receiverParameterType.run {
            isArray() || isAny() || isNullable() || this is IrDynamicType || isString()
        }
    }
}
