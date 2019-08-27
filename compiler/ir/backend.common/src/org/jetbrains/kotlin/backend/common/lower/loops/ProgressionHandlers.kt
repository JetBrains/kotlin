/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.matchers.Quantifier
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/** Builds a [HeaderInfo] for progressions built using the `rangeTo` function. */
internal class RangeToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<IrType>) :
    ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type in progressionElementTypes }
        fqName { it.pathSegments().last() == Name.identifier("rangeTo") }
        parameterCount { it == 1 }
        parameter(0) { it.type in progressionElementTypes }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol) =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = expression.dispatchReceiver!!,
                last = expression.getValueArgument(0)!!,
                step = irInt(1),
                direction = ProgressionDirection.INCREASING
            )
        }
}

/** Builds a [HeaderInfo] for progressions built using the `downTo` extension function. */
internal class DownToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<IrType>) :
    ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type in progressionElementTypes }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = expression.extensionReceiver!!,
                last = expression.getValueArgument(0)!!,
                step = irInt(-1),
                direction = ProgressionDirection.DECREASING
            )
        }
}

/** Builds a [HeaderInfo] for progressions built using the `until` extension function. */
internal class UntilHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<IrType>) :
    ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.until"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type in progressionElementTypes }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // `A until B` is essentially the same as `A .. (B-1)`. However, B could be MIN_VALUE and hence `(B-1)` could underflow.
            // If B is MIN_VALUE, then `A until B` is an empty range. We handle this special case be adding an additional "not empty"
            // condition in the lowered for-loop. Therefore the following for-loop:
            //
            //   for (i in A until B) { // Loop body }
            //
            // is lowered into:
            //
            //   var inductionVar = A
            //   val last = B - 1
            //   if (inductionVar <= last && B != MIN_VALUE) {
            //     // Loop is not empty
            //     do {
            //       val loopVar = inductionVar
            //       inductionVar++
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            //
            // However, `B` may be an expression with side-effects that should only be evaluated once, and `A` may also have side-effects.
            // They are evaluated once and in the correct order (`A` then `B`), the final lowered form is:
            //
            //   // Additional variables
            //   val untilReceiverValue = A
            //   val untilArg = B
            //   // Standard form of loop over progression
            //   var inductionVar = untilReceiverValue
            //   val last = untilArg - 1
            //   if (inductionVar <= last && untilFunArg != MIN_VALUE) {
            //     // Loop is not empty
            //     do {
            //       val loopVar = inductionVar
            //       inductionVar++
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            val receiverValue = expression.extensionReceiver!!
            val untilArg = expression.getValueArgument(0)!!

            // Ensure that the argument conforms to the progression type before we decrement.
            val untilArgCasted = untilArg.castIfNecessary(
                data.elementType(context.irBuiltIns),
                data.elementCastFunctionName
            )

            // To reduce local variable usage, we create and use temporary variables only if necessary.
            var receiverValueVar: IrVariable? = null
            var untilArgVar: IrVariable? = null
            var additionalVariables = emptyList<IrVariable>()
            if (untilArg.canHaveSideEffects) {
                if (receiverValue.canHaveSideEffects) {
                    receiverValueVar = scope.createTemporaryVariable(receiverValue, nameHint = "untilReceiverValue")
                }
                untilArgVar = scope.createTemporaryVariable(untilArgCasted, nameHint = "untilArg")
                additionalVariables = listOfNotNull(receiverValueVar, untilArgVar)
            }

            val first = if (receiverValueVar == null) receiverValue else irGet(receiverValueVar)
            val untilArgExpression = if (untilArgVar == null) untilArgCasted else irGet(untilArgVar)
            val last = untilArgExpression.decrement()

            val (minValueAsLong, minValueIrConst) =
                    when (data) {
                        ProgressionType.INT_PROGRESSION -> Pair(Int.MIN_VALUE.toLong(), irInt(Int.MIN_VALUE))
                        ProgressionType.CHAR_PROGRESSION -> Pair(Char.MIN_VALUE.toLong(), irChar(Char.MIN_VALUE))
                        ProgressionType.LONG_PROGRESSION -> Pair(Long.MIN_VALUE, irLong(Long.MIN_VALUE))
                    }
            val additionalNotEmptyCondition = untilArg.constLongValue.let {
                when {
                    it == null && isAdditionalNotEmptyConditionNeeded(receiverValue.type, untilArg.type) ->
                        // Condition is needed and untilArg is non-const.
                        // Build the additional "not empty" condition: `untilArg != MIN_VALUE`.
                        irNotEquals(untilArgExpression, minValueIrConst)
                    it == minValueAsLong ->
                        // Hardcode "false" as additional condition so that the progression is considered empty.
                        // The entire lowered loop becomes a candidate for dead code elimination, depending on backend.
                        irFalse()
                    else ->
                        // We know that untilArg != MIN_VALUE, so the additional condition is not necessary.
                        null
                }
            }

            ProgressionHeaderInfo(
                data,
                first = first,
                last = last,
                step = irInt(1),
                canOverflow = false,
                additionalVariables = additionalVariables,
                additionalNotEmptyCondition = additionalNotEmptyCondition,
                direction = ProgressionDirection.INCREASING
            )
        }

    private fun isAdditionalNotEmptyConditionNeeded(receiverType: IrType, argType: IrType): Boolean {
        // Here are the available `until` extension functions:
        //
        //   infix fun Char.until(to: Char): CharRange
        //   infix fun Byte.until(to: Byte): IntRange
        //   infix fun Byte.until(to: Short): IntRange
        //   infix fun Byte.until(to: Int): IntRange
        //   infix fun Byte.until(to: Long): LongRange
        //   infix fun Short.until(to: Byte): IntRange
        //   infix fun Short.until(to: Short): IntRange
        //   infix fun Short.until(to: Int): IntRange
        //   infix fun Short.until(to: Long): LongRange
        //   infix fun Int.until(to: Byte): IntRange
        //   infix fun Int.until(to: Short): IntRange
        //   infix fun Int.until(to: Int): IntRange
        //   infix fun Int.until(to: Long): LongRange
        //   infix fun Long.until(to: Byte): LongRange
        //   infix fun Long.until(to: Short): LongRange
        //   infix fun Long.until(to: Int): LongRange
        //   infix fun Long.until(to: Long): LongRange
        //
        // The combinations where the range element type is strictly larger than the argument type do NOT need the additional condition.
        // In such combinations, there is no possibility of underflow when the argument (casted to the range element type) is decremented.
        // For unexpected combinations that currently don't exist (e.g., Int until Char), we assume the check is needed to be safe.
        // TODO: Include unsigned types
        return with(context.irBuiltIns) {
            when (receiverType) {
                charType -> true
                byteType, shortType, intType -> when (argType) {
                    byteType, shortType -> false
                    else -> true
                }
                longType -> when (argType) {
                    byteType, shortType, intType -> false
                    else -> true
                }
                else -> true
            }
        }
    }
}

/** Builds a [HeaderInfo] for progressions built using the `indices` extension property. */
internal class IndicesHandler(private val context: CommonBackendContext) : ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        // TODO: Handle Collection<*>.indices
        // TODO: Handle CharSequence.indices
        extensionReceiver { it != null && it.type.run { isArray() || isPrimitiveArray() } }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // `last = array.size - 1` (last is inclusive) for the loop `for (i in array.indices)`.
            val arraySizeProperty = expression.extensionReceiver!!.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = expression.extensionReceiver
            }.decrement()

            ProgressionHeaderInfo(
                data,
                first = irInt(0),
                last = last,
                step = irInt(1),
                canOverflow = false,
                direction = ProgressionDirection.INCREASING
            )
        }
}

/** Builds a [HeaderInfo] for calls to reverse an iterable. */
internal class ReversedHandler(context: CommonBackendContext, private val visitor: IrElementVisitor<HeaderInfo?, Nothing?>) :
    HeaderInfoFromCallHandler<Nothing?> {

    private val symbols = context.ir.symbols

    // Use Quantifier.ANY so we can handle all reversed iterables in the same manner.
    override val matcher = createIrCallMatcher(Quantifier.ANY) {
        // Matcher for reversed progression.
        callee {
            fqName { it == FqName("kotlin.ranges.reversed") }
            extensionReceiver { it != null && it.type.toKotlinType() in symbols.progressionClassesTypes }
            parameterCount { it == 0 }
        }

        // TODO: Handle reversed String, Progression.withIndex(), etc.
    }

    // Reverse the HeaderInfo from the underlying progression or array (if any).
    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        expression.extensionReceiver!!.accept(visitor, null)?.asReversed()
}

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(private val context: CommonBackendContext) : ExpressionHandler {

    private val symbols = context.ir.symbols

    override fun match(expression: IrExpression) = ProgressionType.fromIrType(expression.type, symbols) != null

    override fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val progression = scope.createTemporaryVariable(expression, nameHint = "progression")
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
                additionalVariables = listOf(progression),
                direction = ProgressionDirection.UNKNOWN
            )
        }
}

internal abstract class IndexedGetIterationHandler(protected val context: CommonBackendContext) : ExpressionHandler {
    override fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Consider the case like:
            //
            //   for (elem in A) { f(elem) }`
            //
            // If we lower it to:
            //
            //   for (i in A.indices) { f(A[i]) }
            //
            // ...then we will break program behaviour if `A` is an expression with side-effect. Instead, we lower it to:
            //
            //   val a = A
            //   for (i in a.indices) { f(a[i]) }
            //
            // This also ensures that the semantics of re-assignment of array variables used in the loop is consistent with the semantics
            // proposed in https://youtrack.jetbrains.com/issue/KT-21354.
            val arrayReference = scope.createTemporaryVariable(
                expression, nameHint = "indexedObject",
                origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
            )

            // `last = array.size` (last is exclusive) for the loop `for (i in array.indices)`.
            val arraySizeProperty = arrayReference.type.getClass()!!.properties.first { it.name.asString() == sizePropertyName() }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = irGet(arrayReference)
            }

            IndexedGetHeaderInfo(
                first = irInt(0),
                last = last,
                step = irInt(1),
                objectVariable = arrayReference
            )
        }

    abstract fun sizePropertyName() : String
}

/** Builds a [HeaderInfo] for arrays. */
internal class ArrayIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context) {
    override fun match(expression: IrExpression) = expression.type.run { isArray() || isPrimitiveArray() }

    override fun sizePropertyName(): String = "size"
}

/** Builds a [HeaderInfo] for iteration over characters in a `CharacterSequence`. */
internal class CharSequenceIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context) {
    override fun match(expression: IrExpression) = expression.type.isSubtypeOfClass(context.ir.symbols.charSequence)

    override fun sizePropertyName(): String = "length"
}