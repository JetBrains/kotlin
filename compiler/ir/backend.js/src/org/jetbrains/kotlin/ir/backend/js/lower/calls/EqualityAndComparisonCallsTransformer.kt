/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name


class EqualityAndComparisonCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    private val symbolToTransformer: SymbolToTransformer = mutableMapOf()

    init {
        symbolToTransformer.run {
            add(irBuiltIns.eqeqeqSymbol, intrinsics.jsEqeqeq)
            add(irBuiltIns.eqeqSymbol, ::transformEqeqOperator)
            // ieee754equals can only be applied in between statically known Floats, Doubles, null or undefined
            add(irBuiltIns.ieee754equalsFunByOperandType, ::chooseEqualityOperatorForPrimitiveTypes)

            add(irBuiltIns.booleanNotSymbol, intrinsics.jsNot)

            add(irBuiltIns.lessFunByOperandType.filterKeys { it != irBuiltIns.long }, intrinsics.jsLt)
            add(irBuiltIns.lessOrEqualFunByOperandType.filterKeys { it != irBuiltIns.long }, intrinsics.jsLtEq)
            add(irBuiltIns.greaterFunByOperandType.filterKeys { it != irBuiltIns.long }, intrinsics.jsGt)
            add(irBuiltIns.greaterOrEqualFunByOperandType.filterKeys { it != irBuiltIns.long }, intrinsics.jsGtEq)

            add(irBuiltIns.lessFunByOperandType[irBuiltIns.long]!!.symbol, transformLongComparison(intrinsics.jsLt))
            add(irBuiltIns.lessOrEqualFunByOperandType[irBuiltIns.long]!!.symbol, transformLongComparison(intrinsics.jsLtEq))
            add(irBuiltIns.greaterFunByOperandType[irBuiltIns.long]!!.symbol, transformLongComparison(intrinsics.jsGt))
            add(irBuiltIns.greaterOrEqualFunByOperandType[irBuiltIns.long]!!.symbol, transformLongComparison(intrinsics.jsGtEq))
        }
    }

    private fun transformLongComparison(comparator: IrSimpleFunction): (IrCall) -> IrExpression = { call ->
        IrCallImpl(
            call.startOffset,
            call.endOffset,
            comparator.returnType,
            comparator.symbol
        ).apply {
            putValueArgument(0, irCall(call, intrinsics.longCompareToLong, firstArgumentAsDispatchReceiver = true))
            putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, 0))
        }
    }

    override fun transformCall(call: IrCall): IrExpression {
        val symbol = call.symbol
        symbolToTransformer[symbol]?.let {
            return it(call)
        }

        return when (symbol.owner.name) {
            Name.identifier("compareTo") -> transformCompareToMethodCall(call)
            Name.identifier("equals") -> transformEqualsMethodCall(call)
            else -> call
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

            !isLhsPrimitive && !lhs.type.isNullable() && equalsMethod != null ->
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
                !isStaticMethodOfClass && parentAsClass.thisReceiver!!.type.isComparable()

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
        if (klass.isEnumClass && klass.isExternal) return null
        return klass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.isEqualsInheritedFromAny() && !it.isFakeOverriddenFromAny() }
            .also { assert(it.size <= 1) }
            .singleOrNull()
    }

    private fun IrFunction.isMethodOfPrimitiveJSType() =
        dispatchReceiverParameter?.let {
            it.type.getPrimitiveType() != PrimitiveType.OTHER
        } ?: false

    private fun IrFunction.isMethodOfPotentiallyPrimitiveJSType() =
        isMethodOfPrimitiveJSType() || isFakeOverriddenFromAny()

}
