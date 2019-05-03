/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.name.Name


class ReflectionCallsTransformer(private val context: JsIrBackendContext) : CallsTransformer {
    private val nameToTransformer: Map<Name, (IrFunctionAccessExpression) -> IrExpression>

    init {
        nameToTransformer = mutableMapOf()
        nameToTransformer.run {
            addWithPredicate(
                Name.special(Namer.KCALLABLE_GET_NAME),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kCallableClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsName, dispatchReceiverAsFirstArgument = true) })

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_GET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsPropertyGet, dispatchReceiverAsFirstArgument = true) }
            )

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_SET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsPropertySet, dispatchReceiverAsFirstArgument = true) }
            )
        }
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression): IrExpression {
        val symbol = call.symbol
        nameToTransformer[symbol.owner.name]?.let {
            return it(call)
        }

        return call
    }
}
