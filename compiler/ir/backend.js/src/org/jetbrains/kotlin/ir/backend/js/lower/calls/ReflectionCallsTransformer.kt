/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicMemberExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.name.Name


class ReflectionCallsTransformer(private val context: JsIrBackendContext) : CallsTransformer {
    private val nameToTransformer: Map<Name, (IrFunctionAccessExpression) -> IrExpression>

    private fun buildDynamicCall(name: String, call: IrFunctionAccessExpression): IrExpression {
        val reference = IrDynamicMemberExpressionImpl(call.startOffset, call.endOffset, context.dynamicType, name, call.dispatchReceiver!!)

        return IrDynamicOperatorExpressionImpl(call.startOffset, call.endOffset, call.type, IrDynamicOperator.INVOKE).apply {
            receiver = reference
            for (i in 0 until call.valueArgumentsCount) {
                arguments += call.getValueArgument(i)!!
            }
        }
    }

    init {
        nameToTransformer = mutableMapOf()
        nameToTransformer.run {
            addWithPredicate(
                Name.special(Namer.KCALLABLE_GET_NAME),
                { call ->
                    call.origin != CallableReferenceLowering.Companion.CALLABLE_REFERENCE_INVOKE &&
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kCallableClass) } ?: false
                },
                { call ->
                    IrDynamicMemberExpressionImpl(
                        call.startOffset,
                        call.endOffset,
                        context.irBuiltIns.stringType,
                        Namer.KCALLABLE_NAME,
                        call.dispatchReceiver!!
                    )
                })

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_GET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> buildDynamicCall(Namer.KPROPERTY_GET, call) }
            )

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_SET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> buildDynamicCall(Namer.KPROPERTY_SET, call) }
            )
        }
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val symbol = call.symbol
        nameToTransformer[symbol.owner.name]?.let {
            return it(call)
        }

        return call
    }
}
