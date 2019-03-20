/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType

// TODO: What if functions like Int.rangeTo(Char) will be added to stdlib later?
internal class RangeToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type.toKotlinType() in progressionElementTypes }
        fqName { it.pathSegments().last() == Name.identifier("rangeTo") }
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType) =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = call.dispatchReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(1)
            )
        }
}

internal class DownToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = call.extensionReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(-1)
            )
        }
}

internal class UntilHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.until"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // `last = bound - 1` for the loop `for (i in first until bound)`.
            val bound = scope.createTemporaryVariable(
                call.getValueArgument(0)!!, nameHint = "bound",
                origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
            )
            val decFun = data.decFun(context.irBuiltIns)
            val last = irCallOp(decFun.symbol, bound.type, call.getValueArgument(0)!!)

            // An additional emptiness condition is required for the corner case:
            //
            // ```
            // for (i in a until MIN_VALUE) {}
            // ```
            //
            // ...which should always be considered an empty range. When the given bound is MIN_VALUE, and because `last = bound - 1`,
            // "last" will underflow to MAX_VALUE, therefore the default emptiness check:
            //
            // ```
            // if (first <= last) { /* loop */ }
            // ```
            //
            // ...will always be true and won't consider the range as empty. Therefore, we need to add an additional condition to the
            // emptiness check so that it becomes:
            //
            // ```
            // if (first <= last && bound > MIN_VALUE) { /* loop */ }
            // ```
            // TODO: Do not add additionalEmptinessCondition if "bound" is const and > MIN_VALUE
            ProgressionHeaderInfo(
                data,
                first = call.extensionReceiver!!,
                last = last,
                step = irInt(1),
                additionalVariables = listOf(bound),
                additionalNotEmptyCondition = buildMinValueConditionIfNecessary(data, irGet(bound))
            )
        }

    private fun DeclarationIrBuilder.buildMinValueConditionIfNecessary(
        progressionType: ProgressionType,
        bound: IrExpression
    ): IrExpression? {
        val irBuiltIns = context.irBuiltIns
        val minConst = when (progressionType) {
            ProgressionType.INT_PROGRESSION -> irInt(Int.MIN_VALUE)
            ProgressionType.CHAR_PROGRESSION -> irChar(Char.MIN_VALUE)
            ProgressionType.LONG_PROGRESSION -> irLong(Long.MIN_VALUE)
        }
        val progressionKotlinType = progressionType.elementType(irBuiltIns).toKotlinType()
        return irCall(irBuiltIns.greaterFunByOperandType[progressionKotlinType]!!).apply {
            putValueArgument(0, bound)
            putValueArgument(1, minConst)
        }
    }
}

internal class IndicesHandler(val context: CommonBackendContext) : ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        // TODO: Handle Collection<*>.indices
        // TODO: Handle CharSequence.indices
        extensionReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // `last = array.size - 1` for the loop `for (i in array.indices)`.
            val arraySizeProperty = call.extensionReceiver!!.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val decFun = data.decFun(context.irBuiltIns)
            val last = irCallOp(decFun.symbol, data.elementType(context.irBuiltIns), irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = call.extensionReceiver
            })

            ProgressionHeaderInfo(
                data,
                first = irInt(0),
                last = last,
                step = irInt(1)
            )
        }
}

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(private val context: CommonBackendContext) : HeaderInfoHandler<Nothing?> {

    private val symbols = context.ir.symbols

    override val matcher = createIrCallMatcher {
        origin { it == IrStatementOrigin.FOR_LOOP_ITERATOR }
        dispatchReceiver { it != null && ProgressionType.fromIrType(it.type, symbols) != null }
    }

    override fun build(call: IrCall, progressionType: Nothing?): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val progression = scope.createTemporaryVariable(call.dispatchReceiver!!)
            val progressionClass = progression.type.getClass()!!
            val firstProperty = progressionClass.properties.first { it.name.asString() == "first" }
            val first = irCall(firstProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val lastProperty = progressionClass.properties.first { it.name.asString() == "last" }
            val last = irCall(lastProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val stepProperty = progressionClass.properties.first { it.name.asString() == "step" }
            val step = irCall(stepProperty.getter!!).apply {
                dispatchReceiver = irGet(progression)
            }

            ProgressionHeaderInfo(
                ProgressionType.fromIrType(progression.type, symbols)!!,
                first,
                last,
                step,
                additionalVariables = listOf(progression)
            )
        }
}

internal class ArrayIterationHandler(private val context: CommonBackendContext) : HeaderInfoHandler<Nothing?> {

    private val intDecFun = ProgressionType.INT_PROGRESSION.decFun(context.irBuiltIns)

    override val matcher = createIrCallMatcher {
        origin { it == IrStatementOrigin.FOR_LOOP_ITERATOR }
        // TODO: Support rare cases like `T : IntArray`
        dispatchReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
    }

    override fun build(call: IrCall, data: Nothing?): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // Consider the case like:
            //
            // ```
            // for (elem in A) { f(elem) }`
            // ```
            //
            // If we lower it to:
            //
            // ```
            // for (i in A.indices) { f(A[i]) }
            // ```
            //
            // ...then we will break program behaviour if `A` is an expression with side-effect. Instead, we lower it to:
            //
            // ```
            // val a = A
            // for (i in a.indices) { f(a[i]) }
            // ```
            val arrayReference = scope.createTemporaryVariable(
                call.dispatchReceiver!!, nameHint = "array",
                origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
            )

            // `last = array.size - 1` for the loop `for (i in array.indices)`.
            val arraySizeProperty = arrayReference.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCallOp(intDecFun.symbol, context.irBuiltIns.intType, irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = irGet(arrayReference)
            })

            ArrayHeaderInfo(
                first = irInt(0),
                last = last,
                step = irInt(1),
                arrayVariable = arrayReference
            )
        }
}

private fun ProgressionType.decFun(builtIns: IrBuiltIns): IrFunction {
    val symbol =
        when (this) {
            ProgressionType.INT_PROGRESSION -> builtIns.intClass
            ProgressionType.LONG_PROGRESSION -> builtIns.longClass
            ProgressionType.CHAR_PROGRESSION -> builtIns.charClass
        }
    return symbol.owner.functions.first { it.name.asString() == "dec" }
}