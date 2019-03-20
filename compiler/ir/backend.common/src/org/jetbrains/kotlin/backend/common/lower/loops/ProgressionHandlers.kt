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
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
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
            //
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
            //
            // TODO: Do not add additionalEmptinessCondition if "bound" is const and > MIN_VALUE
            ProgressionHeaderInfo(
                data,
                first = call.extensionReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(1),
                closed = false,
                additionalNotEmptyCondition = buildMinValueConditionIfNecessary(data, call.getValueArgument(0)!!)
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
            val lowerBound = irInt(0)
            val arrayClass = (call.extensionReceiver!!.type.classifierOrNull) as IrClassSymbol
            val arraySizeProperty = arrayClass.owner.properties.find { it.name.toString() == "size" }!!
            val upperBound = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = call.extensionReceiver
            }
            ProgressionHeaderInfo(
                data,
                lowerBound,
                upperBound,
                step = irInt(1),
                closed = false
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

internal class ArrayIterationHandler(val context: CommonBackendContext) : HeaderInfoHandler<Nothing?> {

    // No support for rare cases like `T : IntArray` for now.
    override val matcher = createIrCallMatcher {
        origin { it == IrStatementOrigin.FOR_LOOP_ITERATOR }
        dispatchReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
    }

    // Consider case like `for (elem in A) { f(elem) }`
    // If we lower it to `for (i in A.indices) { f(A[i]) }`
    // Then we will break program behaviour if A is an expression with side-effect.
    // Instead, we lower it to
    // ```
    // val a = A
    // for (i in a.indices) { f(a[i]) }
    // ```
    override fun build(call: IrCall, data: Nothing?): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            val arrayReference = scope.createTemporaryVariable(call.dispatchReceiver!!)
            val arrayClass = (arrayReference.type.classifierOrNull) as IrClassSymbol
            val arraySizeProperty = arrayClass.owner.properties.find { it.name.toString() == "size" }!!
            val upperBound = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = irGet(arrayReference)
            }
            ArrayHeaderInfo(
                first = irInt(0),
                last = upperBound,
                step = irInt(1),
                arrayVariable = arrayReference
            )
        }
}
