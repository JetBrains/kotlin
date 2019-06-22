/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.matchers.Quantifier
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.math.absoluteValue

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
            //       val i = inductionVar
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
            //   if (inductionVar <= last && untilArg != MIN_VALUE) {
            //     // Loop is not empty
            //     do {
            //       val i = inductionVar
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
                        // Make sure to copy untilArgExpression as it is also used in `last`.
                        irNotEquals(untilArgExpression.deepCopyWithSymbols(), minValueIrConst)
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

/** Builds a [HeaderInfo] for progressions built using the `step` extension function. */
internal class StepHandler(
    private val context: CommonBackendContext,
    private val visitor: IrElementVisitor<HeaderInfo?, Nothing?>
) : ProgressionHandler {

    private val symbols = context.ir.symbols

    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.step"), symbols.progressionClasses.map { it.typeWith() })
        parameter(0) { it.type.isInt() || it.type.isLong() }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Retrieve the HeaderInfo from the underlying progression (if any).
            val nestedInfo = expression.extensionReceiver!!.accept(visitor, null) as? ProgressionHeaderInfo
                ?: return null

            val stepArg = expression.getValueArgument(0)!!
            // We can return the nested info if its step is constant and its absolute value is the same as the step argument. Examples:
            //
            //   1..10 step 1               // Nested step is 1, argument is 1. Equivalent to `1..10`.
            //   10 downTo 1 step 1         // Nested step is -1, argument is 1. Equivalent to `10 downTo 1`.
            //   10 downTo 1 step 2 step 2  // Nested step is -2, argument is 2. Equivalent to `10 downTo 1 step 2`.
            if (stepArg.constLongValue != null && nestedInfo.step.constLongValue?.absoluteValue == stepArg.constLongValue) {
                return nestedInfo
            }

            // To reduce local variable usage, we create and use temporary variables only if necessary.
            var stepArgVar: IrVariable? = null
            val stepArgExpression = if (stepArg.canHaveSideEffects) {
                stepArgVar = scope.createTemporaryVariable(stepArg, nameHint = "stepArg")
                irGet(stepArgVar)
            } else {
                stepArg
            }

            // The `step` standard library function only accepts positive values, and performs the following check:
            //
            //   if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")
            //
            // We insert this check in the lowered form only if necessary.
            val stepType = data.stepType(context.irBuiltIns)
            val stepGreaterFun = context.irBuiltIns.greaterFunByOperandType[stepType.toKotlinType()]!!
            val zeroStep = if (data == ProgressionType.LONG_PROGRESSION) irLong(0) else irInt(0)
            val throwIllegalStepExceptionCall = {
                irCall(context.irBuiltIns.illegalArgumentExceptionSymbol, stepType).apply {
                    val exceptionMessage = irConcat()
                    exceptionMessage.addArgument(irString("Step must be positive, was: "))
                    exceptionMessage.addArgument(stepArgExpression.deepCopyWithSymbols())
                    exceptionMessage.addArgument(irString("."))
                    putValueArgument(0, exceptionMessage)
                }
            }
            val stepArgValueAsLong = stepArgExpression.constLongValue
            val checkedStepExpression = when {
                stepArgValueAsLong == null -> {
                    // Step argument is not a constant.
                    val stepPositiveCheck = irCall(stepGreaterFun).apply {
                        putValueArgument(0, stepArgExpression.deepCopyWithSymbols())
                        putValueArgument(1, zeroStep.deepCopyWithSymbols())
                    }
                    irIfThenElse(
                        stepType,
                        stepPositiveCheck,
                        stepArgExpression.deepCopyWithSymbols(),
                        throwIllegalStepExceptionCall()
                    )
                }
                stepArgValueAsLong > 0L ->
                    // Step argument is a positive constant and is valid.
                    stepArgExpression.deepCopyWithSymbols()
                else ->
                    // Step argument is a non-positive constant and is invalid, directly throw the exception.
                    throwIllegalStepExceptionCall()
            }

            // While the `step` function accepts positive values, the "step" value in the progression depends on the direction of the
            // nested progression. For example, in `10 downTo 1 step 2`, the nested progression is `10 downTo 1` which is decreasing,
            // therefore the step used should be negated (-2).
            //
            // If we don't know the direction of the nested progression (e.g., `someProgression() step 2`) then we have to check its value
            // first to determine whether to negate.
            var nestedStepVar: IrVariable? = null
            var checkedStepVar: IrVariable? = null
            val checkedAndMaybeNegatedStep = when (nestedInfo.direction) {
                ProgressionDirection.INCREASING -> checkedStepExpression
                ProgressionDirection.DECREASING -> checkedStepExpression.negate()
                ProgressionDirection.UNKNOWN -> {
                    // Check value of nested step and negate step arg if needed: `if (nestedStep > 0) nestedStep else -nestedStep`
                    // A temporary variable is created only if necessary, so we can preserve the evaluation order.
                    val nestedStep = nestedInfo.step
                    val nestedStepExpression = if (nestedStep.canHaveSideEffects) {
                        nestedStepVar = scope.createTemporaryVariable(nestedStep, nameHint = "nestedStep")
                        irGet(nestedStepVar)
                    } else {
                        nestedStep
                    }
                    val nestedStepPositiveCheck = irCall(stepGreaterFun).apply {
                        putValueArgument(0, nestedStepExpression)
                        putValueArgument(1, zeroStep.deepCopyWithSymbols())
                    }

                    checkedStepVar = scope.createTemporaryVariable(checkedStepExpression, nameHint = "checkedStep")
                    irIfThenElse(stepType, nestedStepPositiveCheck, irGet(checkedStepVar), irGet(checkedStepVar).negate())
                }
            }

            // Store the final "step" a temporary variable only if necessary, so we can preserve the evaluation order.
            var newStepVar: IrVariable? = null
            val newStepExpression = if (checkedAndMaybeNegatedStep.canHaveSideEffects) {
                newStepVar = scope.createTemporaryVariable(checkedAndMaybeNegatedStep, nameHint = "newStep")
                irGet(newStepVar)
            } else {
                checkedAndMaybeNegatedStep
            }

            // Store the nested "first" and "last" in temporary variables only if necessary, so we can preserve the evaluation order.
            var nestedFirstVar: IrVariable? = null
            val nestedFirst = nestedInfo.first
            val nestedFirstExpression = if (nestedFirst.canHaveSideEffects) {
                nestedFirstVar = scope.createTemporaryVariable(nestedFirst, nameHint = "nestedFirst")
                irGet(nestedFirstVar)
            } else {
                nestedFirst
            }

            var nestedLastVar: IrVariable? = null
            val nestedLast = nestedInfo.last
            val nestedLastExpression = if (nestedLast.canHaveSideEffects) {
                nestedLastVar = scope.createTemporaryVariable(nestedLast, nameHint = "nestedLast")
                irGet(nestedLastVar)
            } else {
                nestedLast
            }

            // Creating a progression with a step value != 1 may result in a "last" value that is smaller than the given "last". The new
            // "last" value is such that iterating over the progression (by incrementing by "step") does not go over the "last" value.
            //
            // For example, in `1..10 step 2`, the values in the progression are [1, 3, 5, 7, 9]. Therefore the "last" value used in the
            // stepped progression should be 9 even though the "last" in the nested progression is 10. Conversely, in `1..10 step 3`, the
            // values in the progression are [1, 4, 7, 10], therefore the "last" value in the stepped progression is still 10. On the other
            // hand, in `1..10 step 10`, the only value in the progression is 1, therefore the "last" value in the progression should be 1.
            // In all cases, the "first" value is unchanged and the nested "first" can be used.
            //
            // The standard library calculates the correct "last" value by calling the internal getProgressionLastElement() function and we
            // do the same when lowering to keep the behavior.
            //
            // In the case of multiple nested steps such as `1..10 step 2 step 3 step 2`, the recalculation happens 3 times:
            //   - In the innermost stepped progression `1..10 step 2`, the values are [1, 3, 5, 7, 9], the new "last" value is 9. (The
            //     return value of `getProgressionLastElement(1, 10, 2)` is 9.)
            //   - For `...step 3`, the values are [1, 4, 7]. It is NOT [1, 4, 7, 10] because the innermost progression stopped at 9. (The
            //     return value of `getProgressionLastElement(1, 9, 3)` is 7.)
            //   - For `...step 2`, the original "last" value of 10 is NOT restored, because the previous step already reduced "last" to 7.
            //     The values are [1, 3, 5, 7], the new "last" value is 7. (The return value of `getProgressionLastElement(1, 7, 2)` is 7.)
            //   - Therefore the final values are: first = 1, last = 7, step = 2. The final "last" is calculated as:
            //       getProgressionLastElement(1,
            //         getProgressionLastElement(1,
            //           getProgressionLastElement(1, 10, 2),
            //         3),
            //       2)
            val recalculatedLast =
                callGetProgressionLastElementIfNecessary(data, nestedFirstExpression, nestedLastExpression, newStepExpression)

            // Consider the following for-loop:
            //
            //   for (i in A..B step C step D) { // Loop body }
            //
            // ...where `A`, `B`, `C`, `D` may have side-effects. Variables will be created for those expressions where necessary, and we
            // must preserve the evaluation order when adding these variables. If all the above expressions can have side-effects (e.g.,
            // function calls), the final lowered form is something like:
            //
            //   // Additional variables for inner step progression `A..B step C`:
            //   val innerNestedFirst = A
            //   val innerNestedLast = B
            //   // No nested step var because step for `A..B` is a constant 1
            //   val innerStepArg = C
            //   val innerNewStep = if (innerStepArg > 0) innerStepArg
            //                      else throw IllegalArgumentException("Step must be positive, was: $innerStepArg.")
            //
            //   // Additional variables for outer step progression `(A..B step C) step D`:
            //   // No nested first var because `innerNestedFirst` is a local variable get (cannot have side-effects)
            //   val outerNestedLast =   // "last" for `A..B step C`
            //       getProgressionLastElement(innerNestedFirst, innerNestedLast, innerNewStep)
            //   // No nested step var because nested step `innerNewStep` is a local variable get (cannot have side-effects)
            //   val outerStepArg = D
            //   val outerNewStep = if (outerStepArg > 0) outerStepArg
            //                      else throw IllegalArgumentException("Step must be positive, was: $outerStepArg.")
            //
            //   // Standard form of loop over progression
            //   var inductionVar = innerNestedFirst
            //   val last =   // "last" for `(A..B step C) step D`
            //       getProgressionLastElement(innerNestedFirst,   // "Passed through" from inner step progression
            //                                 outerNestedLast, outerNewStep)
            //   val step = outerNewStep
            //   if (inductionVar <= last) {
            //     // Loop is not empty
            //     do {
            //       val i = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (i != last)
            //   }
            //
            // Another example (`step` on non-literal progression expression):
            //
            //   for (i in P step C) { // Loop body }
            //
            // ...where `P` and `C` have side-effects. The final lowered form is something like:
            //
            //   // Additional variables:
            //   val progression = P
            //   val nestedFirst = progression.first
            //   val nestedLast = progression.last
            //   val nestedStep = progression.step
            //   val stepArg = C
            //   val checkedStep = if (stepArg > 0) stepArg
            //                     else throw IllegalArgumentException("Step must be positive, was: $stepArg.")
            //   val newStep =   // Direction of P is unknown so we check its step to determine whether to negate
            //       if (nestedStep > 0) checkedStep else -checkedStep
            //
            //   // Standard form of loop over progression
            //   var inductionVar = nestedFirst
            //   val last = getProgressionLastElement(nestedFirst, nestedLast, newStep)
            //   val step = newStep
            //   if ((step > 0 && inductionVar <= last) || (step < 0 && last <= inductionVar)) {
            //     // Loop is not empty
            //     do {
            //       val i = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (i != last)
            //   }
            //
            // If the nested progression is reversed, e.g.:
            //
            //   for (i in (A..B).reversed() step C) { // Loop body }
            //
            // ...in the nested HeaderInfo, "first" is `B` and "last" is `A` (the progression goes from `B` to `A`). Therefore in this case,
            // the nested "last" variable must come before the nested "first" variable (if both variables are necessary).
            val additionalVariables = nestedInfo.additionalVariables + if (nestedInfo.isReversed) {
                listOfNotNull(nestedLastVar, nestedFirstVar, nestedStepVar, stepArgVar, checkedStepVar, newStepVar)
            } else {
                listOfNotNull(nestedFirstVar, nestedLastVar, nestedStepVar, stepArgVar, checkedStepVar, newStepVar)
            }

            return ProgressionHeaderInfo(
                data,
                first = nestedFirstExpression,
                last = recalculatedLast,
                step = newStepExpression,
                isReversed = nestedInfo.isReversed,
                additionalVariables = additionalVariables,
                additionalNotEmptyCondition = nestedInfo.additionalNotEmptyCondition,
                direction = nestedInfo.direction
            )
        }

    private fun DeclarationIrBuilder.callGetProgressionLastElementIfNecessary(
        progressionType: ProgressionType,
        first: IrExpression,
        last: IrExpression,
        step: IrExpression
    ): IrExpression {
        // Calling getProgressionLastElement() is not needed if step == 1 or -1; the "last" value is unchanged in such cases.
        if (step.constLongValue?.absoluteValue == 1L) {
            return last
        }

        // Call `getProgressionLastElement(first, last, step)`
        val stepType = progressionType.stepType(context.irBuiltIns).toKotlinType()
        val getProgressionLastElementFun = symbols.getProgressionLastElementByReturnType[stepType]
            ?: throw IllegalArgumentException("No `getProgressionLastElement` for step type $stepType")
        return irCall(getProgressionLastElementFun).apply {
            putValueArgument(
                0, first.deepCopyWithSymbols().castIfNecessary(
                    progressionType.stepType(context.irBuiltIns),
                    progressionType.stepCastFunctionName
                )
            )
            putValueArgument(
                1, last.deepCopyWithSymbols().castIfNecessary(
                    progressionType.stepType(context.irBuiltIns),
                    progressionType.stepCastFunctionName
                )
            )
            putValueArgument(
                2, step.deepCopyWithSymbols().castIfNecessary(
                    progressionType.stepType(context.irBuiltIns),
                    progressionType.stepCastFunctionName
                )
            )
        }
    }
}

/** Builds a [HeaderInfo] for progressions built using the `indices` extension property. */
internal abstract class IndicesHandler(protected val context: CommonBackendContext) : ProgressionHandler {

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // `last = array.size - 1` (last is inclusive) for the loop `for (i in array.indices)`.
            val receiver = expression.extensionReceiver!!
            val last = irCall(receiver.type.sizePropertyGetter).apply {
                dispatchReceiver = receiver
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

    abstract val IrType.sizePropertyGetter: IrSimpleFunction
}

internal class CollectionIndicesHandler(context: CommonBackendContext) : IndicesHandler(context) {

    override val matcher = SimpleCalleeMatcher {
        extensionReceiver { it != null && it.type.run { isArray() || isPrimitiveArray() || isCollection() } }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override val IrType.sizePropertyGetter
        get() = getClass()!!.getPropertyGetter("size")!!.owner
}

internal class CharSequenceIndicesHandler(context: CommonBackendContext) : IndicesHandler(context) {

    override val matcher = SimpleCalleeMatcher {
        extensionReceiver { it != null && it.type.run { isSubtypeOfClass(context.ir.symbols.charSequence) } }
        fqName { it == FqName("kotlin.text.<get-indices>") }
        parameterCount { it == 0 }
    }

    // The lowering operates on subtypes of CharSequence. Therefore, the IrType could be
    // a type parameter bounded by CharSequence. When that is the case, we cannot get
    // the class from the type and instead uses the CharSequence getter.
    override val IrType.sizePropertyGetter
        get() = getClass()?.getPropertyGetter("length")?.owner ?: context.ir.symbols.charSequence.getPropertyGetter("length")!!.owner
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
            val first = irCall(progressionClass.symbol.getPropertyGetter("first")!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val last = irCall(progressionClass.symbol.getPropertyGetter("last")!!).apply {
                dispatchReceiver = irGet(progression)
            }
            val step = irCall(progressionClass.symbol.getPropertyGetter("step")!!).apply {
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

internal abstract class IndexedGetIterationHandler(
    protected val context: CommonBackendContext,
    val canCacheLast: Boolean = true
) :
    ExpressionHandler {
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
            val objectVariable = scope.createTemporaryVariable(
                expression, nameHint = "indexedObject"
            )

            val last = irCall(expression.type.sizePropertyGetter).apply {
                dispatchReceiver = irGet(objectVariable)
            }

            IndexedGetHeaderInfo(
                first = irInt(0),
                last = last,
                step = irInt(1),
                canCacheLast = canCacheLast,
                objectVariable = objectVariable,
                expressionHandler = this@IndexedGetIterationHandler
            )
        }

    abstract val IrType.sizePropertyGetter: IrSimpleFunction

    abstract val IrType.getFunction: IrSimpleFunction
}

/** Builds a [HeaderInfo] for arrays. */
internal class ArrayIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context) {
    override fun match(expression: IrExpression) = expression.type.run { isArray() || isPrimitiveArray() }

    override val IrType.sizePropertyGetter
        get() = getClass()!!.getPropertyGetter("size")!!.owner

    override val IrType.getFunction
        get() = getClass()!!.functions.first { it.name.asString() == "get" }
}

/** Builds a [HeaderInfo] for iteration over characters in a [String]. */
internal class StringIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context) {
    override fun match(expression: IrExpression) = expression.type.isString()

    override val IrType.sizePropertyGetter
        get() = getClass()!!.getPropertyGetter("length")!!.owner

    override val IrType.getFunction
        get() = getClass()!!.functions.first { it.name.asString() == "get" }
}

/**
 * Builds a [HeaderInfo] for iteration over characters in a [CharSequence].
 *
 * Note: The value for "last" can NOT be cached (i.e., stored in a variable) because the size/length can change within the loop. This means
 * that "last" is re-evaluated with each iteration of the loop.
 */
internal class CharSequenceIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context, canCacheLast = false) {
    override fun match(expression: IrExpression) = expression.type.isSubtypeOfClass(context.ir.symbols.charSequence)

    // The lowering operates on subtypes of CharSequence. Therefore, the IrType could be
    // a type parameter bounded by CharSequence. When that is the case, we cannot get
    // the class from the type and instead uses the CharSequence getter and function.
    override val IrType.sizePropertyGetter
        get() = getClass()?.getPropertyGetter("length")?.owner ?: context.ir.symbols.charSequence.getPropertyGetter("length")!!.owner

    override val IrType.getFunction
        get() = getClass()?.functions?.first { it.name.asString() == "get" }
            ?: context.ir.symbols.charSequence.getSimpleFunction("get")!!.owner
}
