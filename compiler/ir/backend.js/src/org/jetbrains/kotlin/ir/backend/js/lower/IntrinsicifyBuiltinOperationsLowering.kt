/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.name
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

class IntrinsicifyBuiltinOperationsLowering(private val context: JsIrBackendContext) : FileLoweringPass {

    private val primitiveNumberIntrinsics: Map<SimpleMemberKey, IrSimpleFunction>

    init {
        primitiveNumberIntrinsics = mutableMapOf()

        primitiveNumberIntrinsics.run {
            val primitiveNumbers = context.irBuiltIns.run { listOf(int, short, byte, float, double) }

            for (type in primitiveNumbers) {
                binOp(type, OperatorNameConventions.PLUS, context.intrinsics.jsPlus)
                binOp(type, OperatorNameConventions.MINUS, context.intrinsics.jsMinus)
                binOp(type, OperatorNameConventions.TIMES, context.intrinsics.jsMult)
                binOp(type, OperatorNameConventions.DIV, context.intrinsics.jsDiv)
                binOp(type, OperatorNameConventions.MOD, context.intrinsics.jsMod)
                binOp(type, OperatorNameConventions.REM, context.intrinsics.jsMod)
            }
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)

                if (call is IrCall) {
                    val symbol = call.symbol

                    when (symbol) {
                        context.irBuiltIns.eqeqeqSymbol -> {
                            context.intrinsics.jsEqeqeq.symbol
                        }
                        context.irBuiltIns.eqeqSymbol -> {
                            // TODO implement right way
                            context.intrinsics.jsEqeq.symbol
                        }
                        context.irBuiltIns.booleanNotSymbol -> {
                            context.intrinsics.jsNot.symbol
                        }
                        else -> null
                    }?.let {
                        return irCall(call, it)
                    }

                    (symbol.owner as? IrFunction)?.dispatchReceiverParameter?.let {
                        val key = SimpleMemberKey(it.type, symbol.name)

                        primitiveNumberIntrinsics[key]?.let {
                            // TODO: don't apply intrinsics when type of receiver or argument is Long
                            return irCall(call, it.symbol, dispatchReceiverAsFirstArgument = true)
                        }
                    }
                }

                return call
            }
        }, null)
    }
}

// TODO extract to common place?
private fun irCall(call: IrCall, newSymbol: IrFunctionSymbol, dispatchReceiverAsFirstArgument: Boolean = false): IrCall =
    call.run {
        IrCallImpl(
            startOffset,
            endOffset,
            type,
            newSymbol,
            newSymbol.descriptor,
            typeArgumentsCount,
            origin,
            superQualifierSymbol
        ).apply {
            copyTypeAndValueArgumentsFrom(call, dispatchReceiverAsFirstArgument)
        }
    }

// TODO extract to common place?
private fun IrCall.copyTypeAndValueArgumentsFrom(call: IrCall, dispatchReceiverAsFirstArgument: Boolean = false) {
    copyTypeArgumentsFrom(call)

    var j = 0

    if (!dispatchReceiverAsFirstArgument) {
        dispatchReceiver = call.dispatchReceiver
    } else {
        putValueArgument(j++, call.dispatchReceiver)
    }

    extensionReceiver = call.extensionReceiver

    for (i in 0 until call.valueArgumentsCount) {
        putValueArgument(j++, call.getValueArgument(i))
    }
}

private fun <V> MutableMap<SimpleMemberKey, V>.binOp(type: KotlinType, name: Name, v: V) {
    put(SimpleMemberKey(type, name), v)
}

private data class SimpleMemberKey(val klass: KotlinType, val name: Name)
