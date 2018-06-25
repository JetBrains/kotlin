/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.utils.isNullable
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOfClass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.ConversionNames
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.backend.js.utils.isFakeOverriddenFromAny
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType


class IntrinsicifyCallsLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    // TODO: should/can we unify these maps?
    private val memberToTransformer: Map<SimpleMemberKey, (IrCall) -> IrExpression>
    private val memberToIrFunction: Map<SimpleMemberKey, IrSimpleFunctionSymbol>
    private val symbolToIrFunction: Map<IrFunctionSymbol, IrSimpleFunction>
    private val nameToIrTransformer: Map<Name, (IrCall) -> IrExpression>

    init {
        memberToIrFunction = mutableMapOf()
        symbolToIrFunction = mutableMapOf()
        memberToTransformer = mutableMapOf()
        nameToIrTransformer = mutableMapOf()

        val primitiveNumbers = context.irBuiltIns.run { listOf(intType, shortType, byteType, floatType, doubleType) }

        memberToIrFunction.run {
            for (type in primitiveNumbers) {
                op(type, OperatorNames.UNARY_PLUS, intrinsics.jsUnaryPlus)
                op(type, OperatorNames.UNARY_MINUS, intrinsics.jsUnaryMinus)

                op(type, OperatorNames.ADD, intrinsics.jsPlus)
                op(type, OperatorNames.SUB, intrinsics.jsMinus)
                op(type, OperatorNames.MUL, intrinsics.jsMult)
                op(type, OperatorNames.DIV, intrinsics.jsDiv)
                op(type, OperatorNames.MOD, intrinsics.jsMod)
                op(type, OperatorNames.REM, intrinsics.jsMod)
            }

            irBuiltIns.stringType.let {
                op(it, OperatorNames.ADD, intrinsics.jsPlus)
            }

            irBuiltIns.intType.let {
                op(it, OperatorNames.SHL, intrinsics.jsBitShiftL)
                op(it, OperatorNames.SHR, intrinsics.jsBitShiftR)
                op(it, OperatorNames.SHRU, intrinsics.jsBitShiftRU)
                op(it, OperatorNames.AND, intrinsics.jsBitAnd)
                op(it, OperatorNames.OR, intrinsics.jsBitOr)
                op(it, OperatorNames.XOR, intrinsics.jsBitXor)
                op(it, OperatorNames.INV, intrinsics.jsBitNot)
            }

            irBuiltIns.booleanType.let {
                op(it, OperatorNames.AND, intrinsics.jsBitAnd)
                op(it, OperatorNames.OR, intrinsics.jsBitOr)
                op(it, OperatorNames.NOT, intrinsics.jsNot)
                op(it, OperatorNames.XOR, intrinsics.jsBitXor)
            }

            // Conversion rules are ported from NumberAndCharConversionFIF
            // TODO: Add Char and Number conversions

            irBuiltIns.byteType.let {
                op(it, ConversionNames.TO_BYTE, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_DOUBLE, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_FLOAT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_INT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_SHORT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }

            for (type in listOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
                op(type, ConversionNames.TO_BYTE, intrinsics.jsNumberToByte)
                op(type, ConversionNames.TO_DOUBLE, intrinsics.jsAsIs)
                op(type, ConversionNames.TO_FLOAT, intrinsics.jsAsIs)
                op(type, ConversionNames.TO_INT, intrinsics.jsNumberToInt)
                op(type, ConversionNames.TO_SHORT, intrinsics.jsNumberToShort)
                op(type, ConversionNames.TO_LONG, intrinsics.jsNumberToLong)
            }

            irBuiltIns.intType.let {
                op(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
                op(it, ConversionNames.TO_DOUBLE, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_FLOAT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_INT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_SHORT, intrinsics.jsToShort)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }

            irBuiltIns.shortType.let {
                op(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
                op(it, ConversionNames.TO_DOUBLE, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_FLOAT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_INT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_SHORT, intrinsics.jsAsIs)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }
        }

        symbolToIrFunction.run {
            add(irBuiltIns.eqeqeqSymbol, intrinsics.jsEqeqeq)
            // TODO: implement it a right way
            add(irBuiltIns.eqeqSymbol, intrinsics.jsEquals.owner)
            // TODO: implement it a right way
            add(irBuiltIns.ieee754equalsFunByOperandType, intrinsics.jsEqeqeq)

            add(irBuiltIns.booleanNotSymbol, intrinsics.jsNot)

            add(irBuiltIns.lessFunByOperandType, intrinsics.jsLt)
            add(irBuiltIns.lessOrEqualFunByOperandType, intrinsics.jsLtEq)
            add(irBuiltIns.greaterFunByOperandType, intrinsics.jsGt)
            add(irBuiltIns.greaterOrEqualFunByOperandType, intrinsics.jsGtEq)
        }

        memberToTransformer.run {
            for (type in primitiveNumbers) {
                // TODO: use increment and decrement when it's possible
                op(type, OperatorNames.INC) {
                    irCall(it, intrinsics.jsPlus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                        putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, 1))
                    }
                }
                op(type, OperatorNames.DEC) {
                    irCall(it, intrinsics.jsMinus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                        putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, 1))
                    }
                }
            }
        }

        nameToIrTransformer.run {
            addWithPredicate(
                Name.special(Namer.KCALLABLE_GET_NAME),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kCallableClass) } ?: false },
                { call -> irCall(call, context.intrinsics.jsName.symbol, dispatchReceiverAsFirstArgument = true) })

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_GET),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false },
                { call -> irCall(call, context.intrinsics.jsPropertyGet.symbol, dispatchReceiverAsFirstArgument = true)}
            )

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_SET),
                { call -> call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false},
                { call -> irCall(call, context.intrinsics.jsPropertySet.symbol, dispatchReceiverAsFirstArgument = true)}
            )

            addWithPredicate(
                Name.identifier("hashCode"),
                { call -> (call.superQualifier == null) && (call.symbol.owner.descriptor.isFakeOverriddenFromAny()) },
                { call -> irCall(call, intrinsics.jsHashCode, dispatchReceiverAsFirstArgument = true) }
            )

            addWithPredicate(
                Name.identifier("toString"), ::shouldReplaceToStringWithRuntimeCall,
                { call -> irCall(call, intrinsics.jsToString, dispatchReceiverAsFirstArgument = true) }
            )

            addWithPredicate(
                Name.identifier("compareTo"), ::shouldReplaceCompareToWithRuntimeCall,
                { call -> irCall(call, intrinsics.jsCompareTo, dispatchReceiverAsFirstArgument = true) }
            )

            put(Name.identifier("equals"), ::transformEquals)
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            override fun <T> visitConst(expression: IrConst<T>): IrExpression {
                if (expression.kind is IrConstKind.Long) {
                    val value = IrConstKind.Long.valueOf(expression)
                    val high = (value shr 32).toInt()
                    val low = value.toInt()
                    return IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        context.intrinsics.longConstructor
                    ).apply {
                        putValueArgument(0, JsIrBuilder.buildInt(context.irBuiltIns.int, low))
                        putValueArgument(1, JsIrBuilder.buildInt(context.irBuiltIns.int, high))
                    }
                }
                return super.visitConst(expression)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)

                if (call is IrCall) {
                    val symbol = call.symbol

                    if (symbol == irBuiltIns.eqeqSymbol) {
                        val lhs = call.getValueArgument(0)!!
                        val rhs = call.getValueArgument(1)!!

                        return when (translateEquals(lhs.type, rhs.type)) {
                            is IdentityOperator -> irCall(call, intrinsics.jsEqeqeq.symbol)
                            is EqualityOperator -> irCall(call, intrinsics.jsEqeq.symbol)
                            else -> irCall(call, intrinsics.jsEquals)
                        }
                    }

                    symbolToIrFunction[symbol]?.let {
                        return irCall(call, it.symbol)
                    }

                    // TODO: get rid of unbound symbols
                    if (symbol.isBound) {

                        (symbol.owner as? IrFunction)?.dispatchReceiverParameter?.let {
                            val key = SimpleMemberKey(it.type, symbol.owner.name)

                            memberToIrFunction[key]?.let {
                                if (it == intrinsics.jsAsIs) {
                                    return call.dispatchReceiver!!
                                }
                                // TODO: don't apply intrinsics when type of receiver or argument is Long
                                if (call.valueArgumentsCount == 1) {
                                    call.getValueArgument(0)?.let { arg ->
                                        if (arg.type.isLong()) {
                                            call.dispatchReceiver?.type?.let {
                                                if (it.isDouble()) {
                                                    call.putValueArgument(0, IrCallImpl(
                                                        call.startOffset,
                                                        call.endOffset,
                                                        context.intrinsics.longToDouble
                                                    ).apply {
                                                        dispatchReceiver = arg
                                                    })
                                                } else if (it.isFloat()) {
                                                    call.putValueArgument(0, IrCallImpl(
                                                        call.startOffset,
                                                        call.endOffset,
                                                        context.intrinsics.longToFloat
                                                    ).apply {
                                                        dispatchReceiver = arg
                                                    })
                                                }
                                            }
                                        }
                                    }
                                }


                                if (call.valueArgumentsCount == 1 && call.getValueArgument(0)!!.type.isLong()) {
                                    call.dispatchReceiver = IrCallImpl(
                                        call.startOffset,
                                        call.endOffset,
                                        intrinsics.jsNumberToLong
                                    ).apply {
                                        putValueArgument(0, call.dispatchReceiver)
                                    }
                                    return call
                                }
                                return irCall(call, it, dispatchReceiverAsFirstArgument = true)
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

    private fun transformEquals(call: IrCall): IrExpression {
        if (call.superQualifier != null) return call
        val symbol = call.symbol
        if (!symbol.isBound) return call
        val function = (symbol.owner as? IrFunction) ?: return call
        val lhs = function.dispatchReceiverParameter ?: function.extensionReceiverParameter ?: return call
        val rhs = call.getValueArgument(0) ?: return call
        return when (translateEquals(lhs.type, rhs.type)) {
            is IdentityOperator -> irCall(call, intrinsics.jsEqeqeq.symbol)
            is EqualityOperator -> irCall(call, intrinsics.jsEqeq.symbol)
            is RuntimeFunctionCall -> irCall(call, intrinsics.jsEquals, true)
            is RuntimeOrMethodCall -> if (symbol.owner.descriptor.isFakeOverriddenFromAny()) {
                irCall(call, intrinsics.jsEquals, true)
            } else {
                call
            }
        }
    }
}

fun shouldReplaceToStringWithRuntimeCall(call: IrCall): Boolean {
    if (call.superQualifier != null) return false

    // TODO: (KOTLIN-CR-2079)
    //  - User defined extension functions Any?.toString() call can be lost during lowering.
    //  - Use direct method call for dynamic types???
    //  - Define Any?.toString() in runtime library and stop intrincifying extensions

    val receiverParameterType = with(call.symbol.owner) {
        dispatchReceiverParameter ?: extensionReceiverParameter
    }?.type ?: return false

    return receiverParameterType.run {
        this.isArray() ||
                this.isAny() || this.isNullable() || this is IrDynamicType || this.isString()
    }
}

fun shouldReplaceCompareToWithRuntimeCall(call: IrCall): Boolean {
    if (call.superQualifier != null) return false

    // TODO: Replace all compareTo to with runtime call when Comparable<*>.compareTo() bridge is implemented
    return call.symbol.owner.dispatchReceiverParameter?.run {
        type is IrDynamicType
                || type.isJsNumber()
                || type.isNullableJsNumber()
                || type.isBoolean() || type.isNullableBoolean()
                || type.isString() || type.isNullableString()
    } ?: false
}

/*
 Equality translation table:

|                | JsN  | JsN? | Long | Long? | Bool | Bool? | Other | Other? |
|----------------|------|------|------|-------|------|-------|-------|--------|
| JsN            | ===  | ===  | ==   | ==    | ===  | ===   | K.eq  | K.eq   |
| JsN?           | ===  | ==   | ==   | ==    | ===  | K.eq  | K.eq  | K.eq   |
| Long           | ==   | ==   | K.eq | K.eq  | ===  | ===   | K.eq  | K.eq   |
| Long?          | ==   | ==   | K.eq | K.eq  | ===  | K.eq  | K.eq  | K.eq   |
| Bool           | ===  | ===  | ===  | ===   | ===  | ===   | K.eq  | K.eq   |
| Bool?          | ===  | K.eq | ===  | K.eq  | ===  | ==    | K.eq  | K.eq   |
| Other with .eq | .eq  | .eq  | .eq  | .eq   | .eq  | .eq   | .eq   | .eq    |
| Other w/o .eq  | K.eq | K.eq | K.eq | K.eq  | K.eq | K.eq  | K.eq  | K.eq   |
| Other?         | K.eq | K.eq | K.eq | K.eq  | K.eq | K.eq  | K.eq  | K.eq   |


JsNumber -- type lowered to JS Number
    K.eq -- runtime library call
     .eq -- .equals(x) method call

 */

sealed class EqualityLoweringType
class IdentityOperator : EqualityLoweringType()
class EqualityOperator : EqualityLoweringType()
class RuntimeFunctionCall : EqualityLoweringType()
class RuntimeOrMethodCall : EqualityLoweringType()

fun translateEquals(lhs: IrType, rhs: IrType): EqualityLoweringType = when {
    lhs.isNullableNothing() || lhs.isDynamic() -> EqualityOperator()
    lhs.isJsNumber() -> translateEqualsForJsNumber(rhs)
    lhs.isNullableJsNumber() -> translateEqualsForNullableJsNumber(rhs)
    lhs.isLong() -> translateEqualsForLong(rhs)
    lhs.isNullableLong() -> translateEqualsForNullableLong(rhs)
    lhs.isBoolean() -> translateEqualsForBoolean(rhs)
    lhs.isNullableBoolean() -> translateEqualsForNullableBoolean(rhs)
    else -> RuntimeOrMethodCall()
}

fun translateEqualsForJsNumber(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> IdentityOperator()
    rhs.isLong() || rhs.isNullableLong() -> EqualityOperator()
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator()
    else -> RuntimeFunctionCall()
}

fun translateEqualsForNullableJsNumber(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() -> IdentityOperator()
    rhs.isNullableJsNumber() -> EqualityOperator()
    rhs.isLong() || rhs.isNullableLong() -> EqualityOperator()
    rhs.isBoolean() -> IdentityOperator()
    else -> RuntimeFunctionCall()
}

fun translateEqualsForLong(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> EqualityOperator()
    rhs.isLong() || rhs.isNullableLong() -> RuntimeFunctionCall()
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator()
    else -> RuntimeFunctionCall()
}

fun translateEqualsForNullableLong(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> EqualityOperator()
    rhs.isLong() || rhs.isNullableLong() -> RuntimeFunctionCall()
    rhs.isBoolean() -> IdentityOperator()
    else -> RuntimeFunctionCall()
}

fun translateEqualsForBoolean(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> IdentityOperator()
    rhs.isLong() || rhs.isNullableLong() -> IdentityOperator()
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator()
    else -> RuntimeFunctionCall()
}

fun translateEqualsForNullableBoolean(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() -> IdentityOperator()
    rhs.isNullableJsNumber() -> RuntimeFunctionCall()
    rhs.isLong() -> IdentityOperator()
    rhs.isNullableLong() -> RuntimeFunctionCall()
    rhs.isBoolean() -> IdentityOperator()
    rhs.isNullableBoolean() -> EqualityOperator()
    else -> RuntimeFunctionCall()
}


private fun IrType.isNullableJsNumber(): Boolean = isNullablePrimitiveType() && !isNullableLong()

private fun IrType.isJsNumber(): Boolean = isPrimitiveType() && !isLong()


// TODO extract to common place?
fun irCall(call: IrCall, newSymbol: IrFunctionSymbol, dispatchReceiverAsFirstArgument: Boolean = false): IrCall =
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

private fun MutableMap<SimpleMemberKey, IrSimpleFunctionSymbol>.op(type: IrType, name: Name, v: IrSimpleFunctionSymbol) {
    put(SimpleMemberKey(type, name), v)
}

private fun MutableMap<SimpleMemberKey, IrSimpleFunctionSymbol>.op(type: IrType, name: Name, v: IrSimpleFunction) {
    put(SimpleMemberKey(type, name), v.symbol)
}

private fun MutableMap<SimpleMemberKey, (IrCall) -> IrExpression>.op(type: IrType, name: Name, v: (IrCall) -> IrExpression) {
    put(SimpleMemberKey(type, name), v)
}


// TODO issue: marked as unused, but used; rename works wrongly.
private fun <V> MutableMap<SimpleMemberKey, V>.op(type: IrType, name: String, v: V) {
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

private fun <K> MutableMap<K, (IrCall) -> IrExpression>.addWithPredicate(
    from: K,
    predicate: (IrCall) -> Boolean,
    action: (IrCall) -> IrExpression
) {
    put(from) { call: IrCall -> select({ predicate(call) }, { action(call) }, { call }) }
}

private inline fun <T> select(crossinline predicate: () -> Boolean, crossinline ifTrue: () -> T, crossinline ifFalse: () -> T): T = if (predicate()) ifTrue() else ifFalse()

private class SimpleMemberKey(val klass: IrType, val name: Name) {
    // TODO drop custom equals and hashCode when IrTypes will have right equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleMemberKey

        if (name != other.name) return false
        if (klass.originalKotlinType != other.klass.originalKotlinType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = klass.originalKotlinType?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }
}
