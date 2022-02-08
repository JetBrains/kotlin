/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class NumericForLoopHeader<T : NumericHeaderInfo>(
    val headerInfo: T,
    builder: DeclarationIrBuilder,
    protected val context: CommonBackendContext
) : ForLoopHeader {

    override val consumesLoopVariableComponents = false

    val inductionVariable: IrVariable

    protected val stepVariable: IrVariable?
    val stepExpression: IrExpression

    protected val lastVariableIfCanCacheLast: IrVariable?
    protected val lastExpression: IrExpression
        // If this is not `IrExpressionWithCopy`, then it is `<IrGetValue>.getSize()` built in `IndexedGetIterationHandler`.
        // It is therefore safe to deep-copy as it does not contain any functions or classes.
        get() = field.shallowCopyOrNull() ?: field.deepCopyWithSymbols()

    protected val symbols = context.ir.symbols

    init {
        with(builder) {
            with(headerInfo.progressionType) {
                // For this loop:
                //
                //   for (i in first()..last() step step())
                //
                // We need to cast first(), last(). and step() to conform to the progression type so
                // that operations on the induction variable within the loop are more efficient.
                //
                // In the above example, if first() is a Long and last() is an Int, this creates a
                // LongProgression so last() should be cast to a Long.
                inductionVariable =
                    scope.createTmpVariable(
                        headerInfo.first.asElementType(),
                        nameHint = inductionVariableName,
                        isMutable = true,
                        origin = this@NumericForLoopHeader.context.inductionVariableOrigin,
                        irType = elementClass.defaultType
                    )

                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast
                // them to non-nullable.
                // TODO: Confirm if casting to non-nullable is still necessary
                val last = headerInfo.last.asElementType()

                if (headerInfo.canCacheLast) {
                    val (variable, expression) = createLoopTemporaryVariableIfNecessary(last, nameHint = "last")
                    lastVariableIfCanCacheLast = variable
                    lastExpression = expression.shallowCopy()
                } else {
                    lastVariableIfCanCacheLast = null
                    lastExpression = last
                }

                val (tmpStepVar, tmpStepExpression) =
                    createLoopTemporaryVariableIfNecessary(
                        ensureNotNullable(headerInfo.step.asStepType()),
                        nameHint = "step",
                        irType = stepClass.defaultType
                    )
                stepVariable = tmpStepVar
                stepExpression = tmpStepExpression
            }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
        if (expression.type is IrSimpleType && expression.type.isNullable()) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }

    /** Statement used to increment the induction variable. */
    protected fun incrementInductionVariable(builder: DeclarationIrBuilder): IrStatement = with(builder) {
        with(headerInfo.progressionType) {
            // inductionVariable = inductionVariable + step
            // NOTE: We cannot use `stepExpression.type` to match the value parameter type because it may be of type `Nothing`.
            // This happens in the case of an illegal step where the "step" is actually a `throw IllegalArgumentException(...)`.
            val stepType = stepClass.defaultType
            val plusFun = elementClass.defaultType.getClass()!!.functions.single {
                it.name == OperatorNameConventions.PLUS &&
                        it.valueParameters.size == 1 &&
                        it.valueParameters[0].type == stepType
            }
            irSet(
                inductionVariable.symbol, irCallOp(
                    plusFun.symbol, plusFun.returnType,
                    irGet(inductionVariable),
                    stepExpression.shallowCopy(), IrStatementOrigin.PLUSEQ
                ), IrStatementOrigin.PLUSEQ
            )
        }
    }

    protected fun buildLoopCondition(builder: DeclarationIrBuilder): IrExpression {
        with(builder) {
            with(headerInfo.progressionType) {
                val builtIns = context.irBuiltIns

                // Bounds are signed for unsigned progressions but bound comparisons should be done as unsigned, to ensure that the
                // correct comparison function is used (`UInt/ULongCompare`). Also, `compareTo` must be used for UInt/ULong;
                // they don't have intrinsic comparison operators.
                val intCompFun = if (headerInfo.isLastInclusive) {
                    builtIns.lessOrEqualFunByOperandType.getValue(builtIns.intClass)
                } else {
                    builtIns.lessFunByOperandType.getValue(builtIns.intClass)
                }
                val unsignedCompareToFun = if (this is UnsignedProgressionType) {
                    unsignedType.getClass()!!.functions.single {
                        it.name == OperatorNameConventions.COMPARE_TO &&
                                it.dispatchReceiverParameter != null && it.extensionReceiverParameter == null &&
                                it.valueParameters.size == 1 && it.valueParameters[0].type == unsignedType
                    }
                } else null

                val elementCompFun =
                    if (headerInfo.isLastInclusive) {
                        builtIns.lessOrEqualFunByOperandType[elementClass.symbol]
                    } else {
                        builtIns.lessFunByOperandType[elementClass.symbol]
                    }

                fun conditionForDecreasing(): IrExpression =
                    // last <= inductionVar (use `<` if last is exclusive)
                    if (this is UnsignedProgressionType) {
                        irCall(intCompFun).apply {
                            putValueArgument(0, irCall(unsignedCompareToFun!!).apply {
                                dispatchReceiver = lastExpression.asUnsigned()
                                putValueArgument(0, irGet(inductionVariable).asUnsigned())
                            })
                            putValueArgument(1, irInt(0))
                        }
                    } else {
                        irCall(elementCompFun!!).apply {
                            putValueArgument(0, lastExpression)
                            putValueArgument(1, irGet(inductionVariable))
                        }
                    }

                fun conditionForIncreasing(): IrExpression =
                    // inductionVar <= last (use `<` if last is exclusive)
                    if (this is UnsignedProgressionType) {
                        irCall(intCompFun).apply {
                            putValueArgument(0, irCall(unsignedCompareToFun!!).apply {
                                dispatchReceiver = irGet(inductionVariable).asUnsigned()
                                putValueArgument(0, lastExpression.asUnsigned())
                            })
                            putValueArgument(1, irInt(0))
                        }
                    } else {
                        irCall(elementCompFun!!).apply {
                            putValueArgument(0, irGet(inductionVariable))
                            putValueArgument(1, lastExpression)
                        }
                    }

                // The default condition depends on the direction.
                return when (headerInfo.direction) {
                    ProgressionDirection.DECREASING -> conditionForDecreasing()
                    ProgressionDirection.INCREASING -> conditionForIncreasing()
                    ProgressionDirection.UNKNOWN -> {
                        // If the direction is unknown, we check depending on the "step" value:
                        //   // (use `<` if last is exclusive)
                        //   (step > 0 && inductionVar <= last) || (step < 0 || last <= inductionVar)
                        context.oror(
                            context.andand(
                                irCall(builtIns.greaterFunByOperandType.getValue(stepClass.symbol)).apply {
                                    putValueArgument(0, stepExpression.shallowCopy())
                                    putValueArgument(1, zeroStepExpression())
                                },
                                conditionForIncreasing()
                            ),
                            context.andand(
                                irCall(builtIns.lessFunByOperandType.getValue(stepClass.symbol)).apply {
                                    putValueArgument(0, stepExpression.shallowCopy())
                                    putValueArgument(1, zeroStepExpression())
                                },
                                conditionForDecreasing()
                            )
                        )
                    }
                }
            }
        }
    }
}