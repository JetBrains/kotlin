/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOf
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOfClass
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.ConversionNames
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.isNullable

private typealias MemberToTransformer = MutableMap<SimpleMemberKey, (IrCall) -> IrExpression>
private typealias SymbolToTransformer = MutableMap<IrFunctionSymbol, (IrCall) -> IrExpression>


class IntrinsicifyCallsLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    // TODO: should/can we unify these maps?
    private val memberToTransformer: MemberToTransformer
    private val symbolToTransformer: SymbolToTransformer
    private val nameToTransformer: Map<Name, (IrCall) -> IrExpression>
    private val dynamicCallOriginToIrFunction: Map<IrStatementOrigin, IrSimpleFunction>

    init {
        symbolToTransformer = mutableMapOf()
        memberToTransformer = mutableMapOf()
        nameToTransformer = mutableMapOf()
        dynamicCallOriginToIrFunction = mutableMapOf()

        val primitiveNumbers = context.irBuiltIns.run { listOf(intType, shortType, byteType, floatType, doubleType) }

        memberToTransformer.run {
            for (type in primitiveNumbers) {
                op(type, OperatorNames.UNARY_PLUS, intrinsics.jsUnaryPlus)
                op(type, OperatorNames.UNARY_MINUS, intrinsics.jsUnaryMinus)
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
                op(it, ConversionNames.TO_BYTE, ::useDispatchReceiver)
                op(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
                op(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_INT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_SHORT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }

            for (type in listOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
                op(type, ConversionNames.TO_BYTE, intrinsics.jsNumberToByte)
                op(type, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
                op(type, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
                op(type, ConversionNames.TO_INT, intrinsics.jsNumberToInt)
                op(type, ConversionNames.TO_SHORT, intrinsics.jsNumberToShort)
                op(type, ConversionNames.TO_LONG, intrinsics.jsNumberToLong)
            }

            irBuiltIns.intType.let {
                op(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
                op(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
                op(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_INT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_SHORT, intrinsics.jsToShort)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }

            irBuiltIns.shortType.let {
                op(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
                op(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
                op(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_INT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_SHORT, ::useDispatchReceiver)
                op(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
            }

            for (type in primitiveNumbers) {
                op(type, Name.identifier("rangeTo"), ::transformRangeTo)
            }
        }

        symbolToTransformer.run {
            add(irBuiltIns.eqeqeqSymbol, intrinsics.jsEqeqeq)
            add(irBuiltIns.eqeqSymbol, ::transformEqeqOperator)

            // ieee754equals can only be applied in between statically known Floats or Doubles
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

            for (type in primitiveNumbers) {
                op(type, OperatorNames.ADD, withLongCoercion(intrinsics.jsPlus))
                op(type, OperatorNames.SUB, withLongCoercion(intrinsics.jsMinus))
                op(type, OperatorNames.MUL, withLongCoercion(intrinsics.jsMult))
                op(type, OperatorNames.DIV, withLongCoercion(intrinsics.jsDiv))
                op(type, OperatorNames.MOD, withLongCoercion(intrinsics.jsMod))
                op(type, OperatorNames.REM, withLongCoercion(intrinsics.jsMod))
            }

            for (type in arrayOf(irBuiltIns.byteType, irBuiltIns.intType)) {
                op(type, ConversionNames.TO_CHAR) {
                    irCall(it, intrinsics.charConstructor, dispatchReceiverAsFirstArgument = true)
                }
            }

            for (type in arrayOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
                op(type, ConversionNames.TO_CHAR) {
                    IrCallImpl(
                        it.startOffset,
                        it.endOffset,
                        irBuiltIns.charType,
                        intrinsics.charConstructor
                    ).apply {
                        putValueArgument(0, irCall(it, intrinsics.jsNumberToInt, dispatchReceiverAsFirstArgument = true))
                    }
                }
            }

            op(irBuiltIns.charType, ConversionNames.TO_CHAR) { it.dispatchReceiver!! }
        }

        nameToTransformer.run {
            addWithPredicate(
                Name.special(Namer.KCALLABLE_GET_NAME),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kCallableClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsName.symbol, dispatchReceiverAsFirstArgument = true) })

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_GET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsPropertyGet.symbol, dispatchReceiverAsFirstArgument = true) }
            )

            addWithPredicate(
                Name.identifier(Namer.KPROPERTY_SET),
                { call ->
                    call.symbol.owner.dispatchReceiverParameter?.run { type.isSubtypeOfClass(context.irBuiltIns.kPropertyClass) } ?: false
                },
                { call -> irCall(call, context.intrinsics.jsPropertySet.symbol, dispatchReceiverAsFirstArgument = true) }
            )


            addWithPredicate(
                Name.identifier("compareTo"), ::shouldReplaceCompareToWithRuntimeCall,
                { call -> irCall(call, intrinsics.jsCompareTo, dispatchReceiverAsFirstArgument = true) }
            )

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


            put(Name.identifier("equals"), ::transformEqualsMethodCall)
        }

        dynamicCallOriginToIrFunction.run {
            put(IrStatementOrigin.EXCL, context.intrinsics.jsNot)

            put(IrStatementOrigin.LT, context.intrinsics.jsLt)
            put(IrStatementOrigin.GT, context.intrinsics.jsGt)
            put(IrStatementOrigin.LTEQ, context.intrinsics.jsLtEq)
            put(IrStatementOrigin.GTEQ, context.intrinsics.jsGtEq)

            put(IrStatementOrigin.EQEQ, context.intrinsics.jsEqeq)
            put(IrStatementOrigin.EQEQEQ, context.intrinsics.jsEqeqeq)
            put(IrStatementOrigin.EXCLEQ, context.intrinsics.jsNotEq)
            put(IrStatementOrigin.EXCLEQEQ, context.intrinsics.jsNotEqeq)

            put(IrStatementOrigin.ANDAND, context.intrinsics.jsAnd)
            put(IrStatementOrigin.OROR, context.intrinsics.jsOr)

            put(IrStatementOrigin.UMINUS, context.intrinsics.jsUnaryMinus)
            put(IrStatementOrigin.UPLUS, context.intrinsics.jsUnaryPlus)

            put(IrStatementOrigin.PLUS, context.intrinsics.jsPlus)
            put(IrStatementOrigin.MINUS, context.intrinsics.jsMinus)
            put(IrStatementOrigin.MUL, context.intrinsics.jsMult)
            put(IrStatementOrigin.DIV, context.intrinsics.jsDiv)
            put(IrStatementOrigin.PERC, context.intrinsics.jsMod)

            put(IrStatementOrigin.PLUSEQ, context.intrinsics.jsPlusAssign)
            put(IrStatementOrigin.MINUSEQ, context.intrinsics.jsMinusAssign)
            put(IrStatementOrigin.MULTEQ, context.intrinsics.jsMultAssign)
            put(IrStatementOrigin.DIVEQ, context.intrinsics.jsDivAssign)
            put(IrStatementOrigin.PERCEQ, context.intrinsics.jsModAssign)

            put(IrStatementOrigin.PREFIX_INCR, context.intrinsics.jsPrefixInc)
            put(IrStatementOrigin.PREFIX_DECR, context.intrinsics.jsPrefixDec)
            put(IrStatementOrigin.POSTFIX_INCR, context.intrinsics.jsPostfixInc)
            put(IrStatementOrigin.POSTFIX_DECR, context.intrinsics.jsPostfixDec)
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {

            // TODO should this be a separate lowering?
            override fun <T> visitConst(expression: IrConst<T>): IrExpression {
                if (expression.kind is IrConstKind.Long) {
                    val value = IrConstKind.Long.valueOf(expression)
                    val high = (value shr 32).toInt()
                    val low = value.toInt()
                    return IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        irBuiltIns.longType,
                        context.intrinsics.longConstructor
                    ).apply {
                        putValueArgument(0, JsIrBuilder.buildInt(context.irBuiltIns.intType, low))
                        putValueArgument(1, JsIrBuilder.buildInt(context.irBuiltIns.intType, high))
                    }
                } else if (expression.kind is IrConstKind.Char) {
                    return IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        irBuiltIns.charType,
                        context.intrinsics.charConstructor
                    ).apply {
                        putValueArgument(0, JsIrBuilder.buildInt(context.irBuiltIns.intType, IrConstKind.Char.valueOf(expression).toInt()))
                    }
                }
                return super.visitConst(expression)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)

                if (call is IrCall) {
                    val symbol = call.symbol
                    val declaration = symbol.owner

                    if (declaration.isDynamic() || declaration.isEffectivelyExternal()) {
                        when (call.origin) {
                            IrStatementOrigin.GET_PROPERTY -> {
                                val fieldSymbol = context.symbolTable.lazyWrapper.referenceField(
                                    (symbol.descriptor as PropertyAccessorDescriptor).correspondingProperty
                                )
                                return JsIrBuilder.buildGetField(fieldSymbol, call.dispatchReceiver, type = call.type)
                            }

                            // assignment to a property
                            IrStatementOrigin.EQ -> {
                                if (symbol.descriptor is PropertyAccessorDescriptor) {
                                    val fieldSymbol = context.symbolTable.lazyWrapper.referenceField(
                                        (symbol.descriptor as PropertyAccessorDescriptor).correspondingProperty
                                    )
                                    return call.run {
                                        JsIrBuilder.buildSetField(fieldSymbol, dispatchReceiver, getValueArgument(0)!!, type)
                                    }
                                }
                            }
                        }
                    }

                    if (declaration.isDynamic()) {
                        dynamicCallOriginToIrFunction[call.origin]?.let {
                            return irCall(call, it.symbol, dispatchReceiverAsFirstArgument = true)
                        }
                    }

                    symbolToTransformer[symbol]?.let {
                        return it(call)
                    }

                    (symbol.owner as? IrFunction)?.dispatchReceiverParameter?.let {
                        val key = SimpleMemberKey(it.type, symbol.owner.name)
                        memberToTransformer[key]?.let {
                            return it(call)
                        }
                    }

                    nameToTransformer[symbol.owner.name]?.let {
                        return it(call)
                    }
                }

                return call
            }
        }, null)
    }

    private fun useDispatchReceiver(call: IrCall): IrExpression {
        return call.dispatchReceiver!!
    }

    private fun transformRangeTo(call: IrCall): IrExpression {
        if (call.valueArgumentsCount != 1) return call
        return with(call.symbol.owner.valueParameters[0].type) {
            when {
                isByte() || isShort() || isInt() ->
                    irCall(call, intrinsics.jsNumberRangeToNumber, dispatchReceiverAsFirstArgument = true)
                isLong() ->
                    irCall(call, intrinsics.jsNumberRangeToLong, dispatchReceiverAsFirstArgument = true)
                else -> call
            }
        }
    }

    private fun transformEqeqOperator(call: IrCall): IrExpression {
        val lhs = call.getValueArgument(0)!!
        val rhs = call.getValueArgument(1)!!

        // Special optimization for "<expression> == null"
        if (lhs.isNullConst() || rhs.isNullConst())
            return irCall(call, intrinsics.jsEqeq.symbol)

        return when (translateEquals(lhs.type, rhs.type)) {
            is IdentityOperator -> irCall(call, intrinsics.jsEqeqeq.symbol)
            is EqualityOperator -> irCall(call, intrinsics.jsEqeq.symbol)
            is RuntimeFunctionCall -> irCall(call, intrinsics.jsEquals)
            is RuntimeOrMethodCall -> {
                assert(!lhs.type.isNullable())
                val equalsMethod = lhs.type.findEqualsMethod(rhs.type)
                if (equalsMethod != null) {
                    irCall(call, equalsMethod.symbol, firstArgumentAsDispatchReceiver = true)
                } else {
                    irCall(call, intrinsics.jsEquals)
                }
            }
        }
    }

    private fun IrType.findEqualsMethod(rhs: IrType): IrSimpleFunction? {
        val classifier = classifierOrNull ?: return null
        return ((classifier.owner as? IrClass) ?: return null).declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter {
                it.name == Name.identifier("equals")
                        && it.valueParameters.size == 1
                        && rhs.isSubtypeOf(it.valueParameters[0].type)
                        && !it.isFakeOverriddenFromAny()
            }
            .maxWith(  // Find the most specific function
                Comparator { f1, f2 ->
                    val t1 = f1.valueParameters[0].type
                    val t2 = f2.valueParameters[0].type
                    if (t1.isSubtypeOf(t2)) {
                        if (t2.isSubtypeOf(t1)) 0 else 1
                    } else -1
                }
            )
    }

    private fun withLongCoercion(intrinsic: IrSimpleFunction): (IrCall) -> IrExpression = { call ->
        assert(call.valueArgumentsCount == 1)
        val arg = call.getValueArgument(0)!!
        val receiverType = call.dispatchReceiver!!.type

        if (arg.type.isLong()) {
            when {
            // Double OP Long => Double OP Long.toDouble()
                receiverType.isDouble() -> {
                    call.putValueArgument(0, IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        context.intrinsics.longToDouble.owner.returnType,
                        context.intrinsics.longToDouble
                    ).apply {
                        dispatchReceiver = arg
                    })
                }
            // Float OP Long => Float OP Long.toFloat()
                receiverType.isFloat() -> {
                    call.putValueArgument(0, IrCallImpl(

                        call.startOffset,
                        call.endOffset,
                        context.intrinsics.longToFloat.owner.returnType,
                        context.intrinsics.longToFloat
                    ).apply {
                        dispatchReceiver = arg
                    })
                }
            // {Byte, Short, Int} OP Long => {Byte, Sort, Int}.toLong() OP Long
                !receiverType.isLong() -> {
                    call.dispatchReceiver = IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        intrinsics.jsNumberToLong.owner.returnType,
                        intrinsics.jsNumberToLong
                    ).apply {
                        putValueArgument(0, call.dispatchReceiver)
                    }
                }
            }
        }

        if (receiverType.isLong()) {
            // LHS is Long => use as is
            call
        } else {
            irCall(call, intrinsic.symbol, dispatchReceiverAsFirstArgument = true)
        }
    }

    private fun transformEqualsMethodCall(call: IrCall): IrExpression {
        val symbol = call.symbol
        val function = (symbol.owner as? IrFunction) ?: return call
        val lhs = function.dispatchReceiverParameter ?: function.extensionReceiverParameter ?: return call
        val rhs = call.getValueArgument(0) ?: return call
        return when (translateEquals(lhs.type, rhs.type)) {
            is IdentityOperator -> irCall(call, intrinsics.jsEqeqeq.symbol)
            is EqualityOperator -> irCall(call, intrinsics.jsEqeq.symbol)
            is RuntimeFunctionCall -> irCall(call, intrinsics.jsEquals, true)
            is RuntimeOrMethodCall -> if (symbol.owner.isFakeOverriddenFromAny()) {
                if (call.isSuperToAny()) {
                    irCall(call, intrinsics.jsEqeqeq.symbol, dispatchReceiverAsFirstArgument = true)
                } else {
                    irCall(call, intrinsics.jsEquals, dispatchReceiverAsFirstArgument = true)
                }
            } else {
                call
            }
        }
    }
}

fun shouldReplaceToStringWithRuntimeCall(call: IrCall): Boolean {
    // TODO: (KOTLIN-CR-2079)
    //  - User defined extension functions Any?.toString() call can be lost during lowering.
    //  - Use direct method call for dynamic types???
    //  - Define Any?.toString() in runtime library and stop intrincifying extensions

    if (call.valueArgumentsCount > 0)
        return false

    val receiverParameterType = with(call.symbol.owner) {
        dispatchReceiverParameter ?: extensionReceiverParameter
    }?.type ?: return false

    return receiverParameterType.run {
        isArray() || isAny() || isNullable() || this is IrDynamicType || isString()
    }
}

fun shouldReplaceCompareToWithRuntimeCall(call: IrCall): Boolean {
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

|                | JsN  | JsN? | Long | Long? | Bool | Bool? | String | String? | Other | Other? |
|----------------|------|------|------|-------|------|-------|--------|---------|-------|--------|
| JsN            | ===  | ===  | ==   | ==    | ===  | ===   | ===    | ===     | K.eq  | K.eq   |
| JsN?           | ===  | ==   | ==   | ==    | ===  | K.eq  | ===    | K.eq    | K.eq  | K.eq   |
| Long           | ==   | ==   | K.eq | K.eq  | ===  | ===   | ===    | ===     | K.eq  | K.eq   |
| Long?          | ==   | ==   | K.eq | K.eq  | ===  | K.eq  | ===    | K.eq    | K.eq  | K.eq   |
| Bool           | ===  | ===  | ===  | ===   | ===  | ===   | ===    | ===     | K.eq  | K.eq   |
| Bool?          | ===  | K.eq | ===  | K.eq  | ===  | ==    | ===    | K.eq    | K.eq  | K.eq   |
| String         | ===  | ===  | ===  | ===   | ===  | ===   | ===    | ===     | K.eq  | K.eq   |
| String?        | ===  | K.eq | ===  | K.eq  | ===  | K.eq  | ===    | ==      | K.eq  | K.eq   |
| Other with .eq | .eq  | .eq  | .eq  | .eq   | .eq  | .eq   | .eq    | .eq     | .eq   | .eq    |
| Other w/o .eq  | K.eq | K.eq | K.eq | K.eq  | K.eq | K.eq  | K.eq   | K.eq    | K.eq  | K.eq   |
| Other?         | K.eq | K.eq | K.eq | K.eq  | K.eq | K.eq  | K.eq   | K.eq    | K.eq  | K.eq   |


JsNumber -- type lowered to JS Number
    K.eq -- runtime library call
     .eq -- .equals(x) method call

 */

sealed class EqualityLoweringType
object IdentityOperator : EqualityLoweringType()
object EqualityOperator : EqualityLoweringType()
object RuntimeFunctionCall : EqualityLoweringType()
object RuntimeOrMethodCall : EqualityLoweringType()

fun translateEquals(lhs: IrType, rhs: IrType): EqualityLoweringType = when {
    lhs is IrDynamicType -> EqualityOperator
    lhs.isJsNumber() -> translateEqualsForJsNumber(rhs)
    lhs.isNullableJsNumber() -> translateEqualsForNullableJsNumber(rhs)
    lhs.isLong() -> translateEqualsForLong(rhs)
    lhs.isNullableLong() -> translateEqualsForNullableLong(rhs)
    lhs.isBoolean() -> translateEqualsForBoolean(rhs)
    lhs.isNullableBoolean() -> translateEqualsForNullableBoolean(rhs)
    lhs.isString() -> translateEqualsForString(rhs)
    lhs.isNullableString() -> translateEqualsForNullableString(rhs)
    // TODO: Fix unbound symbols (in inline)
    lhs.toKotlinType().isNullable() -> RuntimeFunctionCall
    else -> RuntimeOrMethodCall
}

fun translateEqualsForJsNumber(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> IdentityOperator
    rhs.isLong() || rhs.isNullableLong() -> EqualityOperator
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator
    rhs.isString() || rhs.isNullableString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForNullableJsNumber(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() -> IdentityOperator
    rhs.isNullableJsNumber() -> EqualityOperator
    rhs.isLong() || rhs.isNullableLong() -> EqualityOperator
    rhs.isBoolean() || rhs.isString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForLong(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> EqualityOperator
    rhs.isLong() || rhs.isNullableLong() -> RuntimeFunctionCall
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator
    rhs.isString() || rhs.isNullableString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForNullableLong(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> EqualityOperator
    rhs.isLong() || rhs.isNullableLong() -> RuntimeFunctionCall
    rhs.isBoolean() -> IdentityOperator
    rhs.isString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForBoolean(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> IdentityOperator
    rhs.isLong() || rhs.isNullableLong() -> IdentityOperator
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator
    rhs.isString() || rhs.isNullableString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForNullableBoolean(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() -> IdentityOperator
    rhs.isNullableJsNumber() -> RuntimeFunctionCall
    rhs.isLong() -> IdentityOperator
    rhs.isNullableLong() -> RuntimeFunctionCall
    rhs.isBoolean() -> IdentityOperator
    rhs.isNullableBoolean() -> EqualityOperator
    rhs.isString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForString(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() || rhs.isNullableJsNumber() -> IdentityOperator
    rhs.isLong() || rhs.isNullableLong() -> IdentityOperator
    rhs.isBoolean() || rhs.isNullableBoolean() -> IdentityOperator
    rhs.isString() || rhs.isNullableString() -> IdentityOperator
    else -> RuntimeFunctionCall
}

fun translateEqualsForNullableString(rhs: IrType): EqualityLoweringType = when {
    rhs.isJsNumber() -> IdentityOperator
    rhs.isNullableJsNumber() -> RuntimeFunctionCall
    rhs.isLong() -> IdentityOperator
    rhs.isNullableLong() -> RuntimeFunctionCall
    rhs.isBoolean() -> IdentityOperator
    rhs.isNullableBoolean() -> RuntimeFunctionCall
    rhs.isString() -> IdentityOperator
    rhs.isNullableString() -> EqualityOperator
    else -> RuntimeFunctionCall
}



private fun IrType.isNullableJsNumber(): Boolean = isNullablePrimitiveType() && !isNullableLong() && !isNullableChar()

private fun IrType.isJsNumber(): Boolean = isPrimitiveType() && !isLong() && !isChar()


// TODO extract to common place?
fun irCall(
    call: IrCall,
    newSymbol: IrFunctionSymbol,
    dispatchReceiverAsFirstArgument: Boolean = false,
    firstArgumentAsDispatchReceiver: Boolean = false
): IrCall =
    call.run {
        IrCallImpl(
            startOffset,
            endOffset,
            type,
            newSymbol,
            newSymbol.descriptor,
            typeArgumentsCount,
            origin
        ).apply {
            copyTypeAndValueArgumentsFrom(
                call,
                dispatchReceiverAsFirstArgument,
                firstArgumentAsDispatchReceiver
            )
        }
    }

// TODO extract to common place?
private fun IrCall.copyTypeAndValueArgumentsFrom(
    call: IrCall,
    dispatchReceiverAsFirstArgument: Boolean = false,
    firstArgumentAsDispatchReceiver: Boolean = false
) {
    copyTypeArgumentsFrom(call)

    var toValueArgumentIndex = 0
    var fromValueArgumentIndex = 0

    when {
        dispatchReceiverAsFirstArgument -> {
            putValueArgument(toValueArgumentIndex++, call.dispatchReceiver)
        }
        firstArgumentAsDispatchReceiver -> {
            dispatchReceiver = call.getValueArgument(fromValueArgumentIndex++)
        }
        else -> {
            dispatchReceiver = call.dispatchReceiver
        }
    }

    extensionReceiver = call.extensionReceiver

    while (fromValueArgumentIndex < call.valueArgumentsCount) {
        putValueArgument(toValueArgumentIndex++, call.getValueArgument(fromValueArgumentIndex++))
    }
}

private fun MemberToTransformer.op(type: IrType, name: Name, v: IrSimpleFunctionSymbol) {
    op(type, name, v = { irCall(it, v, dispatchReceiverAsFirstArgument = true) })
}

private fun MemberToTransformer.op(type: IrType, name: Name, v: IrSimpleFunction) {
    op(type, name, v.symbol)
}

private fun MemberToTransformer.op(type: IrType, name: Name, v: (IrCall) -> IrExpression) {
    put(SimpleMemberKey(type, name), v)
}


// TODO issue: marked as unused, but used; rename works wrongly.
private fun <V> MutableMap<SimpleMemberKey, V>.op(type: IrType, name: String, v: V) {
    put(SimpleMemberKey(type, Name.identifier(name)), v)
}

private fun SymbolToTransformer.add(from: Map<SimpleType, IrSimpleFunction>, to: IrSimpleFunction) {
    from.forEach { _, func ->
        add(func.symbol, to)
    }
}

private fun SymbolToTransformer.add(from: IrFunctionSymbol, to: (IrCall) -> IrExpression) {
    put(from, to)
}

private fun SymbolToTransformer.add(from: IrFunctionSymbol, to: IrSimpleFunction) {
    put(from, { call -> irCall(call, to.symbol) })
}

private fun <K> MutableMap<K, (IrCall) -> IrExpression>.addWithPredicate(
    from: K,
    predicate: (IrCall) -> Boolean,
    action: (IrCall) -> IrExpression
) {
    put(from) { call: IrCall -> select({ predicate(call) }, { action(call) }, { call }) }
}

private inline fun <T> select(crossinline predicate: () -> Boolean, crossinline ifTrue: () -> T, crossinline ifFalse: () -> T): T =
    if (predicate()) ifTrue() else ifFalse()

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
