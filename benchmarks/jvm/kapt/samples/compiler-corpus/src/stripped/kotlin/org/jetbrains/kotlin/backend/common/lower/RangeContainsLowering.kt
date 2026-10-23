/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.loops.handlers.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.andand
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Optimizes calls to `contains` (`in` operator) for [ClosedRange]s.
 *
 * For example, the expression `X in A..B` is transformed into `A <= X && X <= B`.
 */
class RangeContainsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}

private class Transformer(
    val context: CommonBackendContext,
    val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {
    private val headerInfoBuilder = RangeHeaderInfoBuilder(context, this::getScopeOwnerSymbol)
    fun getScopeOwnerSymbol() = currentScope?.scope?.scopeOwnerSymbol ?: container.symbol

    private fun matchStdlibExtensionContainsCall(expression: IrCall): Boolean { TODO() }

    override fun visitCall(expression: IrCall): IrExpression { TODO() }

    private fun DeclarationIrBuilder.buildContainsComparison(
        headerInfo: HeaderInfo,
        argument: IrExpression,
        origin: IrStatementOrigin
    ): IrExpression? { TODO() }

    private fun computeComparisonClass(
        irBuiltIns: IrBuiltIns,
        lowerType: IrType,
        upperType: IrType,
        argumentType: IrType
    ): IrClass? { TODO() }

    private fun leastCommonPrimitiveNumericType(irBuiltIns: IrBuiltIns, type1: IrType, type2: IrType): IrType? { TODO() }

    private fun PrimitiveType?.isPromotableToInt(): Boolean = TODO()

    private fun UnsignedType?.isPromotableToUInt(): Boolean = TODO()
}

internal open class RangeHeaderInfoBuilder(context: CommonBackendContext, scopeOwnerSymbol: () -> IrSymbol) :
    HeaderInfoBuilder(context, scopeOwnerSymbol, allowUnsignedBounds = true) {

    override val progressionHandlers = listOf(
        CollectionIndicesHandler(context),
        ArrayIndicesHandler(context),
        CharSequenceIndicesHandler(context),
        UntilHandler(context),
        RangeUntilHandler(context),
        DownToHandler(context),
        RangeToHandler(context)
    )

    override val callHandlers = listOf(
        FloatingPointRangeToHandler,
        ComparableRangeToHandler(context),
        ReversedHandler(context, this)
    )

    override val expressionHandlers = listOf(DefaultProgressionHandler(context, allowUnsignedBounds = true))
}

/** Builds a [HeaderInfo] for closed floating-point ranges built using the `rangeTo` function. */
internal object FloatingPointRangeToHandler : HeaderInfoHandler<IrCall, Nothing?> {
    override fun matchIterable(expression: IrCall): Boolean { TODO() }

    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        FloatingPointRangeHeaderInfo(
            start = expression.arguments[0]!!,
            endInclusive = expression.arguments[1]!!
        )
}

/** Builds a [HeaderInfo] for ranges of Comparables built using the `rangeTo` extension function. */
internal class ComparableRangeToHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrCall, Nothing?> {
    override fun matchIterable(expression: IrCall): Boolean { TODO() }

    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        ComparableRangeInfo(
            start = expression.arguments[0]!!,
            endInclusive = expression.arguments[1]!!
        )
}
