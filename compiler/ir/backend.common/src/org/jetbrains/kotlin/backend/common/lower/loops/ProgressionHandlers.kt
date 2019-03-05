/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType

// TODO: What if functions like Int.rangeTo(Char) will be added to stdlib later?
internal class RangeToHandler(val progressionElementTypes: Collection<SimpleType>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type.toKotlinType() in progressionElementTypes }
        fqName { it.pathSegments().last() == Name.identifier("rangeTo") }
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType) =
        ProgressionHeaderInfo(data, call.dispatchReceiver!!, call.getValueArgument(0)!!)
}

internal class DownToHandler(val progressionElementTypes: Collection<SimpleType>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementTypes)
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        ProgressionHeaderInfo(
            data,
            call.extensionReceiver!!,
            call.getValueArgument(0)!!,
            increasing = false
        )
}

internal class UntilHandler(val progressionElementTypes: Collection<SimpleType>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.until"), progressionElementTypes)
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        ProgressionHeaderInfo(
            data,
            call.extensionReceiver!!,
            call.getValueArgument(0)!!,
            closed = false
        )
}

internal class IndicesHandler(val context: CommonBackendContext) : ProgressionHandler {

    override val matcher = createIrCallMatcher {
        callee {
            fqName { it == FqName("kotlin.collections.<get-indices>") }
            parameterCount { it == 0 }
        }
        // TODO: Handle Collection<*>.indices
        // TODO: Handle CharSequence.indices
        extensionReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            val lowerBound = irInt(0)
            val arrayClass = (call.extensionReceiver!!.type.classifierOrNull) as IrClassSymbol
            val arraySizeProperty = arrayClass.owner.properties.find { it.name.toString() == "size" }!!
            val upperBound = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = call.extensionReceiver
            }
            ProgressionHeaderInfo(data, lowerBound, upperBound, closed = false)
        }

}

internal class StepHandler(context: CommonBackendContext, val visitor: IrElementVisitor<HeaderInfo?, Nothing?>) : ProgressionHandler {
    private val symbols = context.ir.symbols

    override val matcher: IrCallMatcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.step"), symbols.progressionClassesTypes)
        parameter(0) { it.type.isInt() || it.type.isLong() }
    }

    private fun isDefaultStep(step: IrExpression?) =
        step == null || (step is IrConst<*> && step.isOne())

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? {
        val nestedInfo = call.extensionReceiver!!.accept(visitor, null)
            ?: return null

        // Due to KT-27607 nested non-default steps could lead to incorrect behaviour.
        // So disable optimization of such rare cases for now.
        if (!isDefaultStep(nestedInfo.step)) {
            return null
        }

        val newStep = call.getValueArgument(0)!!
        val (newStepCheck, needBoundCalculation) = irCheckProgressionStep(symbols, data, newStep)
        return ProgressionHeaderInfo(
            data,
            nestedInfo.lowerBound,
            nestedInfo.upperBound,
            newStepCheck,
            nestedInfo.increasing,
            needBoundCalculation,
            nestedInfo.closed
        )
    }

    private fun IrConst<*>.isOne() = when (kind) {
        IrConstKind.Long -> value as Long == 1L
        IrConstKind.Int -> value as Int == 1
        else -> false
    }

    private fun IrExpression.isPositiveConst() = this is IrConst<*> &&
            ((kind == IrConstKind.Long && value as Long > 0) || (kind == IrConstKind.Int && value as Int > 0))

    // Used only by the assert.
    private fun stepHasRightType(step: IrExpression, progressionType: ProgressionType) = when (progressionType) {

        ProgressionType.CHAR_PROGRESSION,
        ProgressionType.INT_PROGRESSION -> step.type.makeNotNull().isInt()

        ProgressionType.LONG_PROGRESSION -> step.type.makeNotNull().isLong()
    }

    private fun irCheckProgressionStep(symbols: Symbols<CommonBackendContext>, progressionType: ProgressionType, step: IrExpression) =
        if (step.isPositiveConst()) {
            step to !(step as IrConst<*>).isOne()
        } else
            TODO("Implement call to checkProgressionStep")
//        {
//            // The frontend checks if the step has a right type (Long for LongProgression and Int for {Int/Char}Progression)
//            // so there is no need to cast it.
//            assert(stepHasRightType(step, progressionType))
//
//            val symbol = symbols.checkProgressionStep[step.type.makeNotNull().toKotlinType()]
//                ?: throw IllegalArgumentException("No `checkProgressionStep` for type ${step.type}")
//            IrCallImpl(step.startOffset, step.endOffset, symbol.owner.returnType, symbol).apply {
//                putValueArgument(0, step)
//            } to true
//        }
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
            ArrayHeaderInfo(irInt(0), upperBound, arrayReference)
        }
}
