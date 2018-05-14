/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.util.OperatorNameConventions

class IntrinsicifyCallsLowering(private val context: JsIrBackendContext) : FileLoweringPass {

    // TODO: should/can we unify these maps?
    private val memberToTransformer: Map<SimpleMemberKey, (IrCall) -> IrCall>
    private val memberToIrFunction: Map<SimpleMemberKey, IrSimpleFunction>
    private val symbolToIrFunction: Map<IrFunctionSymbol, IrSimpleFunction>
    private val nameToIrTransformer: Map<Name, (IrCall) -> IrCall>

    val kCallable = context.builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kCallable.toSafe())
    val kProperty = context.builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kProperty.asSingleFqName())

    init {
        memberToIrFunction = mutableMapOf()
        symbolToIrFunction = mutableMapOf()
        memberToTransformer = mutableMapOf()
        nameToIrTransformer = mutableMapOf()

        val primitiveNumbers = context.irBuiltIns.run { listOf(int, short, byte, float, double) }

        memberToIrFunction.run {
            for (type in primitiveNumbers) {
                op(type, OperatorNameConventions.UNARY_PLUS, context.intrinsics.jsUnaryPlus)
                op(type, OperatorNameConventions.UNARY_MINUS, context.intrinsics.jsUnaryMinus)

                op(type, OperatorNameConventions.PLUS, context.intrinsics.jsPlus)
                op(type, OperatorNameConventions.MINUS, context.intrinsics.jsMinus)
                op(type, OperatorNameConventions.TIMES, context.intrinsics.jsMult)
                op(type, OperatorNameConventions.DIV, context.intrinsics.jsDiv)
                op(type, OperatorNameConventions.MOD, context.intrinsics.jsMod)
                op(type, OperatorNameConventions.REM, context.intrinsics.jsMod)
            }

            context.irBuiltIns.string.let {
                op(it, OperatorNameConventions.PLUS, context.intrinsics.jsPlus)
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

        symbolToIrFunction.run {
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

        memberToTransformer.run {
            for (type in primitiveNumbers) {
                // TODO: use increment and decrement when it's possible
                op(type, OperatorNameConventions.INC) {
                    irCall(it, context.intrinsics.jsPlus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                        putValueArgument(1, IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.int, IrConstKind.Int, 1))
                    }
                }
                op(type, OperatorNameConventions.DEC) {
                    irCall(it, context.intrinsics.jsMinus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                        putValueArgument(1, IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.int, IrConstKind.Int, 1))
                    }
                }
            }
        }

        nameToIrTransformer.run {
            addWithPredicate(
                Name.special(Namer.KCALLABLE_GET_NAME),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { DescriptorUtils.isSubtypeOfClass(type, kCallable) } ?: false },
                { call -> irCall(call, context.intrinsics.jsName.symbol, dispatchReceiverAsFirstArgument = true) })

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_GET),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { DescriptorUtils.isSubtypeOfClass(type, kProperty) } ?: false },
                { call -> irCall(call, context.intrinsics.jsPropertyGet.symbol, dispatchReceiverAsFirstArgument = true)}
            )

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_SET),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { DescriptorUtils.isSubtypeOfClass(type, kProperty) } ?: false},
                { call -> irCall(call, context.intrinsics.jsPropertySet.symbol, dispatchReceiverAsFirstArgument = true)}
            )
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)

                if (call is IrCall) {
                    val symbol = call.symbol

                    symbolToIrFunction[symbol]?.let {
                        return irCall(call, it.symbol)
                    }

                    // TODO: get rid of unbound symbols
                    if (symbol.isBound) {

                        (symbol.owner as? IrFunction)?.dispatchReceiverParameter?.let {
                            val key = SimpleMemberKey(it.type, symbol.owner.name)

                            memberToIrFunction[key]?.let {
                                // TODO: don't apply intrinsics when type of receiver or argument is Long
                                return irCall(call, it.symbol, dispatchReceiverAsFirstArgument = true)
                            }

                            memberToTransformer[key]?.let {
                                return it(call)
                            }
                        }

                        nameToIrTransformer[symbol.owner.name]?.let {
                            return it(call)
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

private fun <K> MutableMap<K, (IrCall) -> IrCall>.addWithPredicate(from: K, predicate: (IrCall) -> Boolean, action: (IrCall) -> IrCall) {
    put(from) { call: IrCall -> select({ predicate(call) }, { action(call) }, { call }) }
}

private inline fun <T> select(crossinline predicate: () -> Boolean, crossinline ifTrue: () -> T, crossinline ifFalse: () -> T): T = if (predicate()) ifTrue() else ifFalse()

private data class SimpleMemberKey(val klass: KotlinType, val name: Name)
