/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName
import kotlin.math.absoluteValue

/** Builds a [HeaderInfo] for progressions built using the `step` extension function. */
internal class StepHandler(
    private val context: CommonBackendContext,
    private val visitor: HeaderInfoBuilder
) : ProgressionHandler {

    private val symbols = context.ir.symbols

    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(
            FqName("kotlin.ranges.step"),
            symbols.progressionClasses.map { it.defaultType })
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
            // This temporary variable for step needs to be mutable for certain cases (see below).
            val (stepArgVar, stepArgExpression) = createTemporaryVariableIfNecessary(stepArg, "stepArg", isMutable = true)

            // The `step` standard library function only accepts positive values, and performs the following check:
            //
            //   if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")
            //
            // We insert a similar check in the lowered form only if necessary.
            val stepType = data.stepClass.defaultType
            val stepCompFun = context.irBuiltIns.lessOrEqualFunByOperandType.getValue(data.stepClass.symbol)
            val zeroStep = data.run { zeroStepExpression() }
            val throwIllegalStepExceptionCall = {
                irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                    val exceptionMessage = irConcat()
                    exceptionMessage.addArgument(irString("Step must be positive, was: "))
                    exceptionMessage.addArgument(stepArgExpression.deepCopyWithSymbols())
                    exceptionMessage.addArgument(irString("."))
                    putValueArgument(0, exceptionMessage)
                }
            }
            val stepArgValueAsLong = stepArgExpression.constLongValue
            val stepCheck: IrStatement? = when {
                stepArgValueAsLong == null -> {
                    // Step argument is not a constant. In this case, we check if step <= 0.
                    val stepNonPositiveCheck = irCall(stepCompFun).apply {
                        putValueArgument(0, stepArgExpression.deepCopyWithSymbols())
                        putValueArgument(1, zeroStep.deepCopyWithSymbols())
                    }
                    irIfThen(
                        context.irBuiltIns.unitType,
                        stepNonPositiveCheck,
                        throwIllegalStepExceptionCall()
                    )
                }
                stepArgValueAsLong <= 0L ->
                    // Step argument is a non-positive constant and is invalid, directly throw the exception.
                    throwIllegalStepExceptionCall()
                else ->
                    // Step argument is a positive constant and is valid. No check is needed.
                    null
            }

            // While the `step` function accepts positive values, the "step" value in the progression depends on the direction of the
            // nested progression. For example, in `10 downTo 1 step 2`, the nested progression is `10 downTo 1` which is decreasing,
            // therefore the step used should be negated (-2).
            //
            // If we don't know the direction of the nested progression (e.g., `someProgression() step 2`) then we have to check its value
            // first to determine whether to negate.
            var nestedStepVar: IrVariable? = null
            var stepNegation: IrStatement? = null
            val finalStepExpression = when (nestedInfo.direction) {
                ProgressionDirection.INCREASING -> stepArgExpression
                ProgressionDirection.DECREASING -> {
                    if (stepArgVar == null) {
                        stepArgExpression.negate()
                    } else {
                        // Step is already stored in a variable, just negate it.
                        stepNegation = irSet(stepArgVar.symbol, irGet(stepArgVar).negate())
                        irGet(stepArgVar)
                    }
                }
                ProgressionDirection.UNKNOWN -> {
                    // Check value of nested step and negate step arg if needed: `if (nestedStep <= 0) -step else step`
                    // A temporary variable is created only if necessary, so we can preserve the evaluation order.
                    val nestedStep = nestedInfo.step
                    val (tmpNestedStepVar, nestedStepExpression) = createTemporaryVariableIfNecessary(nestedStep, "nestedStep")
                    nestedStepVar = tmpNestedStepVar
                    val nestedStepNonPositiveCheck = irCall(stepCompFun).apply {
                        putValueArgument(0, nestedStepExpression)
                        putValueArgument(1, zeroStep.deepCopyWithSymbols())
                    }
                    if (stepArgVar == null) {
                        // Create a temporary variable for the possibly-negated step, so we don't have to re-check every time step is used.
                        stepNegation = scope.createTmpVariable(
                            irIfThenElse(
                                stepType,
                                nestedStepNonPositiveCheck,
                                stepArgExpression.deepCopyWithSymbols().negate(),
                                stepArgExpression.deepCopyWithSymbols()
                            ),
                            nameHint = "maybeNegatedStep"
                        )
                        irGet(stepNegation)
                    } else {
                        // Step is already stored in a variable, just negate it.
                        stepNegation = irIfThen(
                            context.irBuiltIns.unitType,
                            nestedStepNonPositiveCheck,
                            irSet(stepArgVar.symbol, irGet(stepArgVar).negate())
                        )
                        irGet(stepArgVar)
                    }
                }
            }

            // Store the nested "first" and "last" and final "step" in temporary variables only if necessary, so we can preserve the
            // evaluation order.
            val (nestedFirstVar, nestedFirstExpression) = createTemporaryVariableIfNecessary(nestedInfo.first, "nestedFirst")
            val (nestedLastVar, nestedLastExpression) = createTemporaryVariableIfNecessary(nestedInfo.last, "nestedLast")

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
                callGetProgressionLastElementIfNecessary(data, nestedFirstExpression, nestedLastExpression, finalStepExpression)

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
            //   if (innerStepArg <= 0) throw IllegalArgumentException("Step must be positive, was: $innerStepArg.")
            //
            //   // Additional variables for outer step progression `(A..B step C) step D`:
            //   // No nested first var because `innerNestedFirst` is a local variable get (cannot have side-effects)
            //   val outerNestedLast =   // "last" for `A..B step C`
            //       getProgressionLastElement(innerNestedFirst, innerNestedLast, innerStepArg)
            //   // No nested step var because nested step `innerStepArg` is a local variable get (cannot have side-effects)
            //   val outerStepArg = D
            //   if (outerStepArg <= 0) throw IllegalArgumentException("Step must be positive, was: $outerStepArg.")
            //
            //   // Standard form of loop over progression
            //   var inductionVar = innerNestedFirst
            //   val last =   // "last" for `(A..B step C) step D`
            //       getProgressionLastElement(innerNestedFirst,   // "Passed through" from inner step progression
            //                                 outerNestedLast, outerStepArg)
            //   if (inductionVar <= last) {
            //     // Loop is not empty
            //     do {
            //       val i = inductionVar
            //       inductionVar += outerStepArg
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
            //   var stepArg = C
            //   if (stepArg <= 0) throw IllegalArgumentException("Step must be positive, was: $stepArg.")
            //   // Direction of P is unknown so we check its step to determine whether to negate
            //   if (nestedStep <= 0) stepArg = -stepArg
            //
            //   // Standard form of loop over progression
            //   var inductionVar = nestedFirst
            //   val last = getProgressionLastElement(nestedFirst, nestedLast, stepArg)
            //   if ((stepArg > 0 && inductionVar <= last) || (stepArg < 0 && last <= inductionVar)) {
            //     // Loop is not empty
            //     do {
            //       val i = inductionVar
            //       inductionVar += stepArg
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
            val additionalStatements = nestedInfo.additionalStatements + if (nestedInfo.isReversed) {
                listOfNotNull(nestedLastVar, nestedFirstVar, nestedStepVar, stepArgVar, stepCheck, stepNegation)
            } else {
                listOfNotNull(nestedFirstVar, nestedLastVar, nestedStepVar, stepArgVar, stepCheck, stepNegation)
            }

            return ProgressionHeaderInfo(
                data,
                first = nestedFirstExpression,
                last = recalculatedLast,
                step = finalStepExpression,
                isReversed = nestedInfo.isReversed,
                additionalStatements = additionalStatements,
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

        // Call `getProgressionLastElement(first, last, step)`. The following overloads are present in the stdlib:
        //   - getProgressionLastElement(Int, Int, Int): Int          // Used by IntProgression and CharProgression (uses Int step)
        //   - getProgressionLastElement(Long, Long, Long): Long      // Used by LongProgression
        //   - getProgressionLastElement(UInt, UInt, Int): UInt       // Used by UIntProgression (uses Int step)
        //   - getProgressionLastElement(ULong, ULong, Long): ULong   // Used by ULongProgression (uses Long step)
        with(progressionType) {
            val getProgressionLastElementFun = getProgressionLastElementFunction
                ?: error("No `getProgressionLastElement` for progression type ${progressionType::class.simpleName}")
            return if (this is UnsignedProgressionType) {
                // Bounds are signed for unsigned progressions but `getProgressionLastElement` expects unsigned.
                // The return value is finally converted back to signed since it will be assigned back to `last`.
                irCall(getProgressionLastElementFun).apply {
                    putValueArgument(0, first.deepCopyWithSymbols().asElementType().asUnsigned())
                    putValueArgument(1, last.deepCopyWithSymbols().asElementType().asUnsigned())
                    putValueArgument(2, step.deepCopyWithSymbols().asStepType())
                }.asSigned()
            } else {
                irCall(getProgressionLastElementFun).apply {
                    // Step type is used for casting because it works for all signed progressions. In particular,
                    // getProgressionLastElement(Int, Int, Int) is called for CharProgression, which uses an Int step.
                    putValueArgument(0, first.deepCopyWithSymbols().asStepType())
                    putValueArgument(1, last.deepCopyWithSymbols().asStepType())
                    putValueArgument(2, step.deepCopyWithSymbols().asStepType())
                }
            }
        }
    }
}