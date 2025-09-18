/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne

class EqualityAndComparisonCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val symbols = context.symbols
    private val irBuiltIns = context.irBuiltIns
    private val icUtils = context.inlineClassesUtils
    private val longAsBigInt = context.configuration.compileLongAsBigint

    private val symbolToTransformer: SymbolToTransformer = hashMapOf()

    init {
        symbolToTransformer.run {
            add(irBuiltIns.eqeqeqSymbol, ::transformEqeqeqOperator)
            add(irBuiltIns.eqeqSymbol, ::transformEqeqOperator)
            // ieee754equals can only be applied in between statically known Floats, Doubles, null or undefined
            add(irBuiltIns.ieee754equalsFunByOperandType, ::chooseEqualityOperatorForPrimitiveTypes)

            add(irBuiltIns.booleanNotSymbol, symbols.jsNot)

            add(irBuiltIns.lessFunByOperandType.filterKeys { it != irBuiltIns.longClass }, symbols.jsLt)
            add(irBuiltIns.lessOrEqualFunByOperandType.filterKeys { it != irBuiltIns.longClass }, symbols.jsLtEq)
            add(irBuiltIns.greaterFunByOperandType.filterKeys { it != irBuiltIns.longClass }, symbols.jsGt)
            add(irBuiltIns.greaterOrEqualFunByOperandType.filterKeys { it != irBuiltIns.longClass }, symbols.jsGtEq)

            add(irBuiltIns.lessFunByOperandType[irBuiltIns.longClass]!!, transformLongComparison(symbols.jsLt))
            add(irBuiltIns.lessOrEqualFunByOperandType[irBuiltIns.longClass]!!, transformLongComparison(symbols.jsLtEq))
            add(irBuiltIns.greaterFunByOperandType[irBuiltIns.longClass]!!, transformLongComparison(symbols.jsGt))
            add(irBuiltIns.greaterOrEqualFunByOperandType[irBuiltIns.longClass]!!, transformLongComparison(symbols.jsGtEq))
        }
    }

    private fun transformLongComparison(comparator: IrSimpleFunctionSymbol): (IrFunctionAccessExpression) -> IrExpression = { call ->
        if (longAsBigInt) {
            irCall(call, comparator)
        } else {
            IrCallImpl(
                call.startOffset,
                call.endOffset,
                comparator.owner.returnType,
                comparator,
                typeArgumentsCount = 0
            ).apply {
                arguments[0] = irCall(call, symbols.longCompareToLong!!)
                arguments[1] = JsIrBuilder.buildInt(irBuiltIns.intType, 0)
            }
        }
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val symbol = call.symbol
        symbolToTransformer[symbol]?.let {
            return it(call)
        }

        return when (symbol.owner.name) {
            Name.identifier("compareTo") -> if (doNotIntrinsify) call else transformCompareToMethodCall(call)
            Name.identifier("equals") -> transformEqualsMethodCall(call as IrCall)
            else -> call
        }
    }

    private fun transformEqeqeqOperator(call: IrFunctionAccessExpression): IrExpression {
        val lhs = call.arguments[0]!!
        val rhs = call.arguments[1]!!

        return if (lhs.isCharBoxing() && rhs.isCharBoxing()) {
            optimizeInlineClassEquality(call, lhs, rhs)
        } else {
            irCall(call, symbols.jsEqeqeq)
        }
    }

    private fun IrExpression.isCharBoxing(): Boolean {
        return this is IrCall && symbol == symbols.jsBoxIntrinsic && arguments[0]!!.type.isChar()
    }

    private fun transformEqeqOperator(call: IrFunctionAccessExpression): IrExpression {
        val lhs = call.arguments[0]!!
        val rhs = call.arguments[1]!!

        val lhsJsType = lhs.type.getPrimitiveType()
        val rhsJsType = rhs.type.getPrimitiveType()

        val equalsMethod = lhs.type.findEqualsMethod()

        return when {
            lhs.type is IrDynamicType ->
                irCall(call, symbols.jsEqeq)

            // Special optimization for "<expression> == null"
            lhs.isNullConst() || rhs.isNullConst() ->
                irCall(call, symbols.jsEqeq)

            // For non-float primitives of the same type use JS `==`
            lhsJsType == rhsJsType && lhsJsType.canBeUsedWithJsEq() ->
                chooseEqualityOperatorForPrimitiveTypes(call)

            lhs.type.isLong() && rhs.type.isLong() -> if (longAsBigInt) {
                chooseEqualityOperatorForPrimitiveTypes(call)
            } else {
                irCall(call, symbols.longEquals!!)
            }

            !lhsJsType.isBuiltin() && !lhs.type.isNullable() && equalsMethod != null ->
                irCall(call, equalsMethod.symbol)

            // For inline class instances we can try to unbox them for the equality comparison
            lhs.isBoxIntrinsic() && rhs.isBoxIntrinsic() ->
                optimizeInlineClassEquality(call, lhs, rhs)

            else ->
                irCall(call, symbols.jsEquals)
        }
    }

    private fun chooseEqualityOperatorForPrimitiveTypes(call: IrFunctionAccessExpression): IrExpression = when {
        call.allValueArgumentsAreNullable() ->
            irCall(call, symbols.jsEqeq)
        else ->
            irCall(call, symbols.jsEqeqeq)
    }

    private fun IrFunctionAccessExpression.allValueArgumentsAreNullable() =
        nonDispatchArguments.all { it!!.type.isNullable() }

    private fun transformCompareToMethodCall(call: IrFunctionAccessExpression): IrExpression {
        val function = call.symbol.owner as IrSimpleFunction
        if (function.parent !is IrClass) return call

        fun IrSimpleFunction.isFakeOverriddenFromComparable(): Boolean = when {
            !isFakeOverride ->
                !isStaticMethodOfClass && parentAsClass.thisReceiver!!.type.isComparable()

            else -> overriddenSymbols.all { it.owner.isFakeOverriddenFromComparable() }
        }

        return when {
            // Use runtime function call in case when receiverType is a primitive JS type that doesn't have `compareTo` method,
            // or has a potential to be primitive type (being fake overridden from `Comparable`)
            function.isMethodOfPrimitiveJSType() || function.isFakeOverriddenFromComparable() ->
                irCall(call, symbols.jsCompareTo)

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
                irCall(call, symbols.jsEqeqeq)

            // Use runtime function call in case when receiverType is a primitive JS type that doesn't have `equals` method,
            // or has a potential to be primitive type (being fake overridden from `Any`)
            function.isMethodOfPotentiallyPrimitiveJSType() ->
                irCall(call, symbols.jsEquals)

            // Valid `equals` method must be present at this point
            else -> call
        }
    }

    private fun IrType.findEqualsMethod(): IrSimpleFunction? {
        val klass = getClass() ?: return null
        if (klass.isEnumClass && klass.isExternal) return null
        return klass.declarations.asSequence()
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.isEqualsInheritedFromAny() && !it.isFakeOverriddenFromAny() }
            .atMostOne()
    }

    private enum class PrimitiveType {
        FLOATING_POINT_NUMBER,
        INTEGER_NUMBER,
        BIGINT_NUMBER,
        STRING,
        BOOLEAN,
        OTHER
    }

    private fun IrType.getPrimitiveType() = makeNotNull().run {
        when {
            isBoolean() -> PrimitiveType.BOOLEAN
            isByte() || isShort() || isInt() -> PrimitiveType.INTEGER_NUMBER
            isFloat() || isDouble() -> PrimitiveType.FLOATING_POINT_NUMBER
            isString() -> PrimitiveType.STRING
            isLong() && longAsBigInt -> PrimitiveType.BIGINT_NUMBER
            else -> PrimitiveType.OTHER
        }
    }


    private fun IrFunction.isMethodOfPrimitiveJSType() =
        dispatchReceiverParameter?.let {
            it.type.getPrimitiveType() != PrimitiveType.OTHER
        } ?: false

    private fun IrFunction.isMethodOfPotentiallyPrimitiveJSType() =
        isMethodOfPrimitiveJSType() || isFakeOverriddenFromAny()

    private fun PrimitiveType.isBuiltin() =
        this != PrimitiveType.OTHER

    private fun PrimitiveType.canBeUsedWithJsEq() =
        isBuiltin() && this != PrimitiveType.FLOATING_POINT_NUMBER

    private fun IrType.isDefaultEqualsMethod() =
        isChar() || findEqualsMethod()?.origin === IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER

    private fun IrExpression.isBoxIntrinsic() =
        this is IrCall && symbol == icUtils.boxIntrinsic

    private fun IrExpression.unboxParamWithInlinedClass(): Pair<IrExpression, IrClass?> {
        val unboxed = (this as IrFunctionAccessExpression).arguments[0]
            ?: irError("Boxed expression is expected") {
                withIrEntry("this", this@unboxParamWithInlinedClass)
            }
        return Pair(unboxed, icUtils.getInlinedClass(unboxed.type))
    }

    private fun optimizeInlineClassEquality(call: IrFunctionAccessExpression, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val (lhsUnboxed, lhsClassType) = lhs.unboxParamWithInlinedClass()
        val (rhsUnboxed, rhsClassType) = rhs.unboxParamWithInlinedClass()
        if (lhsClassType !== null && lhsClassType === rhsClassType && lhsUnboxed.type.isDefaultEqualsMethod()) {
            call.arguments[0] = lhsUnboxed
            call.arguments[1] = rhsUnboxed

            if (lhsUnboxed.type.isChar() || lhsUnboxed.type.getLowestUnderlyingType().getPrimitiveType().canBeUsedWithJsEq()) {
                return chooseEqualityOperatorForPrimitiveTypes(call)
            }
        }

        return irCall(call, symbols.jsEquals)
    }

    private fun IrType.getLowestUnderlyingType(): IrType {
        if (isDefaultEqualsMethod()) {
            val underlyingType = icUtils.getInlinedClass(this)?.inlineClassRepresentation?.underlyingType
            if (underlyingType !== null) {
                return underlyingType.getLowestUnderlyingType()
            }
        }
        return this
    }
}
