/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOfClass
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.irCall
import org.jetbrains.kotlin.ir.backend.js.utils.ConversionNames
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
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
            // ieee754equals can only be applied in between statically known Floats, Doubles, null or undefined
            add(irBuiltIns.ieee754equalsFunByOperandType, ::chooseEqualityOperatorForPrimitiveTypes)

            add(irBuiltIns.booleanNotSymbol, intrinsics.jsNot)

            add(irBuiltIns.lessFunByOperandType, intrinsics.jsLt)
            add(irBuiltIns.lessOrEqualFunByOperandType, intrinsics.jsLtEq)
            add(irBuiltIns.greaterFunByOperandType, intrinsics.jsGt)
            add(irBuiltIns.greaterOrEqualFunByOperandType, intrinsics.jsGtEq)

            // Arrays
            add(context.intrinsics.array.sizeProperty, context.intrinsics.jsArrayLength, true)
            add(context.intrinsics.array.getFunction, context.intrinsics.jsArrayGet, true)
            add(context.intrinsics.array.setFunction, context.intrinsics.jsArraySet, true)
            add(context.intrinsics.array.iterator, context.intrinsics.jsArrayIteratorFunction.owner, true)
            for ((key, elementType) in context.intrinsics.primitiveArrays) {
                add(key.sizeProperty, context.intrinsics.jsArrayLength, true)
                add(key.getFunction, context.intrinsics.jsArrayGet, true)
                add(key.setFunction, context.intrinsics.jsArraySet, true)
                add(key.iterator, context.intrinsics.jsPrimitiveArrayIteratorFunctions[elementType]!!.owner, true)

                // TODO irCall?
                add(key.sizeConstructor) { call ->
                    IrCallImpl(call.startOffset, call.endOffset, call.type, context.intrinsics.primitiveToSizeConstructor[elementType]!!).apply {
                        putValueArgument(0, call.getValueArgument(0))
                    }
                }
            }

            add(context.irBuiltIns.stringClass.lengthProperty, context.intrinsics.jsArrayLength, true)
            add(context.irBuiltIns.stringClass.getFunction, intrinsics.jsCharSequenceGet.owner, true)
            add(context.irBuiltIns.stringClass.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence"}.symbol,
                intrinsics.jsCharSequenceSubSequence.owner, true)

            add(intrinsics.charSequenceLengthPropertyGetterSymbol, intrinsics.jsCharSequenceLength.owner, true)
            add(intrinsics.charSequenceGetFunctionSymbol, intrinsics.jsCharSequenceGet.owner, true)
            add(intrinsics.charSequenceSubSequenceFunctionSymbol, intrinsics.jsCharSequenceSubSequence.owner, true)
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
                    irCall(it, intrinsics.charClassSymbol.constructors.single(), dispatchReceiverAsFirstArgument = true)
                }
            }

            for (type in arrayOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
                op(type, ConversionNames.TO_CHAR) {
                    JsIrBuilder.buildCall(intrinsics.charClassSymbol.constructors.single()).apply {
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

            put(Name.identifier("compareTo"), ::transformCompareToMethodCall)
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
            put(IrStatementOrigin.GET_ARRAY_ELEMENT, context.intrinsics.jsArrayGet)
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            private fun <C> lowerConst(
                irClass: IrClassSymbol,
                carrierFactory: (Int, Int, IrType, C) -> IrExpression,
                vararg args: C
            ): IrExpression {
                val constructor = irClass.constructors.single()
                val argType = constructor.owner.valueParameters.first().type
                return JsIrBuilder.buildCall(constructor).apply {
                    for (i in args.indices) {
                        putValueArgument(i, carrierFactory(UNDEFINED_OFFSET, UNDEFINED_OFFSET, argType, args[i]))
                    }
                }
            }

            private fun createLong(v: Long): IrExpression = lowerConst(context.intrinsics.longClassSymbol, IrConstImpl<*>::int, v.toInt(), (v shr 32).toInt())

            // TODO should this be a separate lowering?
            override fun <T> visitConst(expression: IrConst<T>): IrExpression {
                with(context.intrinsics) {
                    return when (expression.type.classifierOrNull) {
                        uByteClassSymbol -> lowerConst(uByteClassSymbol, IrConstImpl<*>::byte, IrConstKind.Byte.valueOf(expression))

                        uShortClassSymbol -> lowerConst(uShortClassSymbol, IrConstImpl<*>::short, IrConstKind.Short.valueOf(expression))

                        uIntClassSymbol -> lowerConst(uIntClassSymbol, IrConstImpl<*>::int, IrConstKind.Int.valueOf(expression))

                        uLongClassSymbol -> lowerConst(uLongClassSymbol, { _, _, _, v -> createLong(v) }, IrConstKind.Long.valueOf(expression))

                        else -> when {
                            expression.kind is IrConstKind.Char ->
                                lowerConst(charClassSymbol, IrConstImpl<*>::int, IrConstKind.Char.valueOf(expression).toInt())

                            expression.kind is IrConstKind.Long ->
                                createLong(IrConstKind.Long.valueOf(expression))

                            else -> super.visitConst(expression)
                        }
                    }
                }
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(intrinsics.doNotIntrinsifyAnnotationSymbol))
                    return declaration
                return super.visitFunction(declaration)
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


    private fun withLongCoercion(intrinsic: IrSimpleFunction): (IrCall) -> IrExpression = { call ->
        assert(call.valueArgumentsCount == 1)
        val arg = call.getValueArgument(0)!!

        if (arg.type.isLong()) {
            val receiverType = call.dispatchReceiver!!.type

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

        if (call.dispatchReceiver!!.type.isLong()) {
            // LHS is Long => use as is
            call
        } else {
            irCall(call, intrinsic.symbol, dispatchReceiverAsFirstArgument = true)
        }
    }

    private fun transformEqeqOperator(call: IrCall): IrExpression {
        val lhs = call.getValueArgument(0)!!
        val rhs = call.getValueArgument(1)!!


        val lhsJsType = lhs.type.getPrimitiveType()
        val rhsJsType = rhs.type.getPrimitiveType()

        val equalsMethod = lhs.type.findEqualsMethod()
        val isLhsPrimitive = lhsJsType != PrimitiveType.OTHER

        return when {
            lhs.type is IrDynamicType ->
                irCall(call, intrinsics.jsEqeq.symbol)

            // Special optimization for "<expression> == null"
            lhs.isNullConst() || rhs.isNullConst() ->
                irCall(call, intrinsics.jsEqeq.symbol)

            // For non-float primitives of the same type use JS `==`
            isLhsPrimitive && lhsJsType == rhsJsType && lhsJsType != PrimitiveType.FLOATING_POINT_NUMBER ->
                chooseEqualityOperatorForPrimitiveTypes(call)

            !isLhsPrimitive && !lhs.type.toKotlinType().isNullable() && equalsMethod != null ->
                irCall(call, equalsMethod.symbol, firstArgumentAsDispatchReceiver = true)

            else ->
                irCall(call, intrinsics.jsEquals)
        }
    }

    private fun chooseEqualityOperatorForPrimitiveTypes(call: IrCall): IrExpression = when {
        call.allValueArgumentsAreNullable() ->
            irCall(call, intrinsics.jsEqeq.symbol)
        else ->
            irCall(call, intrinsics.jsEqeqeq.symbol)
    }

    private fun IrCall.allValueArgumentsAreNullable() =
        (0 until valueArgumentsCount).all { getValueArgument(it)!!.type.isNullable() }

    private fun transformCompareToMethodCall(call: IrCall): IrExpression {
        val function = call.symbol.owner as IrSimpleFunction
        if (function.parent !is IrClass) return call

        fun IrSimpleFunction.isFakeOverriddenFromComparable(): Boolean = when {
            origin != IrDeclarationOrigin.FAKE_OVERRIDE ->
                parentAsClass.thisReceiver!!.type.isComparable()

            else -> overriddenSymbols.all { it.owner.isFakeOverriddenFromComparable() }
        }

        return when {
            // Use runtime function call in case when receiverType is a primitive JS type that doesn't have `compareTo` method,
            // or has a potential to be primitive type (being fake overridden from `Comparable`)
            function.isMethodOfPrimitiveJSType() || function.isFakeOverriddenFromComparable() ->
                irCall(call, intrinsics.jsCompareTo, dispatchReceiverAsFirstArgument = true)

            // Valid `compareTo` method must be present at this point
            else ->
                call
        }
    }


    private fun transformEqualsMethodCall(call: IrCall): IrExpression {
        val function = call.symbol.owner
        return when {
            // Nothing special
            !function.isEqualsInheritedFromAny() -> call

            // `Any.equals` works as identity operator
            call.isSuperToAny() ->
                irCall(call, intrinsics.jsEqeqeq.symbol, dispatchReceiverAsFirstArgument = true)

            // Use runtime function call in case when receiverType is a primitive JS type that doesn't have `equals` method,
            // or has a potential to be primitive type (being fake overridden from `Any`)
            function.isMethodOfPotentiallyPrimitiveJSType() ->
                irCall(call, intrinsics.jsEquals, dispatchReceiverAsFirstArgument = true)

            // Valid `equals` method must be present at this point
            else -> call
        }
    }

    private fun IrType.findEqualsMethod(): IrSimpleFunction? {
        val klass = getClass() ?: return null
        return klass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.isEqualsInheritedFromAny() && !it.isFakeOverriddenFromAny() }
            .also { assert(it.size <= 1) }
            .singleOrNull()
    }

    private fun IrFunction.isMethodOfPrimitiveJSType() =
        dispatchReceiverParameter?.type?.getPrimitiveType() != PrimitiveType.OTHER

    private fun IrFunction.isMethodOfPotentiallyPrimitiveJSType() =
        isMethodOfPrimitiveJSType() || isFakeOverriddenFromAny()

    private fun IrFunction.isEqualsInheritedFromAny() =
        name == Name.identifier("equals") &&
                dispatchReceiverParameter != null &&
                valueParameters.size == 1 &&
                valueParameters[0].type.isNullableAny()

    private fun shouldReplaceToStringWithRuntimeCall(call: IrCall): Boolean {
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
}

enum class PrimitiveType {
    FLOATING_POINT_NUMBER,
    INTEGER_NUMBER,
    STRING,
    BOOLEAN,
    OTHER
}

fun IrType.getPrimitiveType() = makeNotNull().run {
    when {
        isBoolean() -> PrimitiveType.BOOLEAN
        isByte() || isShort() || isInt() -> PrimitiveType.INTEGER_NUMBER
        isFloat() || isDouble() -> PrimitiveType.FLOATING_POINT_NUMBER
        isString() -> PrimitiveType.STRING
        else -> PrimitiveType.OTHER
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

private fun SymbolToTransformer.add(from: Map<SimpleType, IrSimpleFunction>, to: (IrCall) -> IrExpression) {
    from.forEach { _, func ->
        add(func.symbol, to)
    }
}

private fun SymbolToTransformer.add(from: IrFunctionSymbol, to: (IrCall) -> IrExpression) {
    put(from, to)
}

private fun SymbolToTransformer.add(from: IrFunctionSymbol, to: IrSimpleFunction, dispatchReceiverAsFirstArgument: Boolean = false) {
    put(from, { call -> irCall(call, to.symbol, dispatchReceiverAsFirstArgument) })
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


private val IrClassSymbol.sizeProperty
    get() = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "size" }.getter!!.symbol

private val IrClassSymbol.getFunction
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "get" }.symbol

private val IrClassSymbol.setFunction
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "set" }.symbol

private val IrClassSymbol.iterator
    get() = owner.declarations.filterIsInstance<IrFunction>().first { it.name.asString() == "iterator" }.symbol

private val IrClassSymbol.sizeConstructor
    get() = owner.declarations.filterIsInstance<IrConstructor>().first { it.valueParameters.size == 1 }.symbol

private val IrClassSymbol.lengthProperty
    get() = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "length" }.getter!!.symbol
