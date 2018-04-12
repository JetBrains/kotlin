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
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.util.OperatorNameConventions

class IntrinsicifyBuiltinOperationsLowering(private val context: JsIrBackendContext) : FileLoweringPass {

    // TODO: should/can we unify these maps?
    private val primitiveNumberIntrinsics: Map<SimpleMemberKey, IrSimpleFunction>
    private val comparisonIntrinsics: Map<IrFunctionSymbol, IrSimpleFunction>

    init {
        primitiveNumberIntrinsics = mutableMapOf()
        comparisonIntrinsics = mutableMapOf()

        primitiveNumberIntrinsics.run {
            val primitiveNumbers = context.irBuiltIns.run { listOf(int, short, byte, float, double) }

            for (type in primitiveNumbers) {
                op(type, OperatorNameConventions.UNARY_PLUS, context.intrinsics.jsUnaryPlus)
                op(type, OperatorNameConventions.UNARY_MINUS, context.intrinsics.jsUnaryMinus)

                // TODO: inc & dec are mapped wrongly
                op(type, OperatorNameConventions.INC, context.intrinsics.jsPrefixInc)
                op(type, OperatorNameConventions.DEC, context.intrinsics.jsPrefixDec)

                op(type, OperatorNameConventions.PLUS, context.intrinsics.jsPlus)
                op(type, OperatorNameConventions.MINUS, context.intrinsics.jsMinus)
                op(type, OperatorNameConventions.TIMES, context.intrinsics.jsMult)
                op(type, OperatorNameConventions.DIV, context.intrinsics.jsDiv)
                op(type, OperatorNameConventions.MOD, context.intrinsics.jsMod)
                op(type, OperatorNameConventions.REM, context.intrinsics.jsMod)
            }

            context.irBuiltIns.int.let {
                op(it, "shl", context.intrinsics.jsBitShiftL)
                op(it, "shr", context.intrinsics.jsBitShiftR)
                op(it, "ushr", context.intrinsics.jsBitShiftRU)
                op(it, "and", context.intrinsics.jsBitAnd)
                op(it, "or", context.intrinsics.jsBitOr)
                op(it, "xor", context.intrinsics.jsBitXor)
                op(it, "inv", context.intrinsics.jsBitNot)
            }

            context.irBuiltIns.bool.let {
                op(it, OperatorNameConventions.AND, context.intrinsics.jsAnd)
                op(it, OperatorNameConventions.OR, context.intrinsics.jsOr)
                op(it, OperatorNameConventions.NOT, context.intrinsics.jsNot)
                op(it, "xor", context.intrinsics.jsBitXor)
            }
        }

        comparisonIntrinsics.run {
            add(context.irBuiltIns.eqeqeqSymbol, context.intrinsics.jsEqeqeq)
            // TODO: implement it a right way
            add(context.irBuiltIns.eqeqSymbol, context.intrinsics.jsEqeq)
            // TODO: implement it a right way
            add(context.irBuiltIns.ieee754equalsFunByOperandType, context.intrinsics.jsEqeqeq)

            add(context.irBuiltIns.booleanNotSymbol, context.intrinsics.jsNot)

            add(context.irBuiltIns.lessFunByOperandType, context.intrinsics.jsLt)
            add(context.irBuiltIns.lessOrEqualFunByOperandType, context.intrinsics.jsLtEq)
            add(context.irBuiltIns.greaterFunByOperandType, context.intrinsics.jsGt)
            add(context.irBuiltIns.greaterOrEqualFunByOperandType, context.intrinsics.jsGtEq)
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)

                if (call is IrCall) {
                    val symbol = call.symbol

                    comparisonIntrinsics[symbol]?.let {
                        return irCall(call, it.symbol)
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

private fun <V> MutableMap<SimpleMemberKey, V>.op(type: KotlinType, name: Name, v: V) {
    put(SimpleMemberKey(type, name), v)
}

// TODO issue: marked as unused, but used; rename works wrongly.
private fun <V> MutableMap<SimpleMemberKey, V>.op(type: KotlinType, name: String, v: V) {
    put(SimpleMemberKey(type, Name.identifier(name)), v)
}

private fun <V> MutableMap<IrFunctionSymbol, V>.add(from: Map<SimpleType, IrSimpleFunction>, to: V) {
    from.forEach { _, func ->
        add(func.symbol, to)
    }
}

private fun <V> MutableMap<IrFunctionSymbol, V>.add(from: IrFunctionSymbol, to: V) {
    put(from, to)
}

private data class SimpleMemberKey(val klass: KotlinType, val name: Name)
