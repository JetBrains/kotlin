/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Contains the loop and expression to replace the old loop.
 *
 * @param newLoop The new loop.
 * @param replacementExpression The expression to use in place of the old loop. It is either `newLoop`, or a container
 * that contains `newLoop`.
 */
internal data class LoopReplacement(
    val newLoop: IrLoop,
    val replacementExpression: IrExpression
)

internal interface ForLoopHeader {
    /** Statements used to initialize the entire loop (e.g., declare induction variable). */
    val loopInitStatements: List<IrStatement>

    /**
     * Whether or not [initializeIteration] consumes the loop variable components assigned to it.
     * If true, the component variables should be removed from the un-lowered loop.
     */
    val consumesLoopVariableComponents: Boolean

    /** Statements used to initialize an iteration of the loop (e.g., assign loop variable). */
    fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder
    ): List<IrStatement>

    /** Builds a new loop from the old loop. */
    fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement
}

internal abstract class NumericForLoopHeader<T : NumericHeaderInfo>(
    protected val headerInfo: T,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext,
    protected val isLastInclusive: Boolean
) : ForLoopHeader {

    override val consumesLoopVariableComponents = false

    val inductionVariable: IrVariable

    protected val stepVariable: IrVariable?
    val stepExpression: IrExpression
        // Always copy `stepExpression` is it may be used multiple times.
        get() = field.deepCopyWithSymbols()

    protected val lastVariableIfCanCacheLast: IrVariable?
    protected val lastExpression: IrExpression
        // Always copy `lastExpression` is it may be used in multiple conditions.
        get() = field.deepCopyWithSymbols()

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
                        nameHint = "inductionVariable",
                        isMutable = true,
                        irType = elementClass.defaultType
                    )

                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast
                // them to non-nullable.
                // TODO: Confirm if casting to non-nullable is still necessary
                val last = headerInfo.last.asElementType()

                lastVariableIfCanCacheLast = if (headerInfo.canCacheLast) {
                    scope.createTmpVariable(last, nameHint = "last")
                } else null

                lastExpression = if (headerInfo.canCacheLast) irGet(lastVariableIfCanCacheLast!!) else last

                val (tmpStepVar, tmpStepExpression) =
                    createTemporaryVariableIfNecessary(
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
                    stepExpression
                )
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
                val intCompFun = if (isLastInclusive) {
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
                    if (isLastInclusive) {
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
                                    putValueArgument(0, stepExpression)
                                    putValueArgument(1, zeroStepExpression())
                                },
                                conditionForIncreasing()
                            ),
                            context.andand(
                                irCall(builtIns.lessFunByOperandType.getValue(stepClass.symbol)).apply {
                                    putValueArgument(0, stepExpression)
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

internal class ProgressionLoopHeader(
    headerInfo: ProgressionHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<ProgressionHeaderInfo>(headerInfo, builder, context, isLastInclusive = true) {

    // For this loop:
    //
    //   for (i in first()..last() step step())
    //
    // ...the functions may have side-effects so we need to call them in the following order: first() (inductionVariable), last(), step().
    // Additional variables come first as they may be needed to the subsequent variables.
    //
    // In the case of a reversed range, the `inductionVariable` and `last` variables are swapped, therefore the declaration order must be
    // swapped to preserve the correct evaluation order.
    override val loopInitStatements = headerInfo.additionalStatements + (
            if (headerInfo.isReversed)
                listOfNotNull(lastVariableIfCanCacheLast, inductionVariable)
            else
                listOfNotNull(inductionVariable, lastVariableIfCanCacheLast)
            ) +
            listOfNotNull(stepVariable)

    private var loopVariable: IrVariable? = null

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder
    ) =
        with(builder) {
            // loopVariable is used in the loop condition if it can overflow. If no loopVariable was provided, create one.
            this@ProgressionLoopHeader.loopVariable = if (headerInfo.canOverflow && loopVariable == null) {
                scope.createTmpVariable(
                    irGet(inductionVariable),
                    nameHint = "loopVariable",
                    isMutable = true
                )
            } else {
                loopVariable?.initializer = irGet(inductionVariable).let {
                    headerInfo.progressionType.run {
                        if (this is UnsignedProgressionType) {
                            // The induction variable is signed for unsigned progressions but the loop variable should be unsigned.
                            it.asUnsigned()
                        } else it
                    }
                }
                loopVariable
            }

            // loopVariable = inductionVariable
            // inductionVariable = inductionVariable + step
            listOfNotNull(loopVariable, incrementInductionVariable(this))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        with(builder) {
            val newLoop = if (headerInfo.canOverflow) {
                // If the induction variable CAN overflow, we cannot use it in the loop condition. Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     // Loop is not empty
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (loopVar != last)
                //   }
                IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    val loopVariableExpression = irGet(loopVariable!!).let {
                        headerInfo.progressionType.run {
                            if (this is UnsignedProgressionType) {
                                // The loop variable is signed but bounds are signed for unsigned progressions.
                                it.asSigned()
                            } else it
                        }
                    }
                    label = oldLoop.label
                    condition = irNotEquals(loopVariableExpression, lastExpression)
                    body = newBody
                }
            } else {
                // If the induction variable can NOT overflow, use a do-while loop. Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (inductionVar <= last)
                //   }
                //
                // Even though this can be simplified into a simpler while loop, using if + do-while (i.e., doing a loop inversion)
                // performs better in benchmarks. In cases where `last` is a constant, the `if` may be optimized away.
                IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    label = oldLoop.label
                    condition = buildLoopCondition(this@with)
                    body = newBody
                }
            }

            val loopCondition = buildLoopCondition(this@with)
            // Combine with the additional "not empty" condition, if any.
            val notEmptyCheck =
                irIfThen(headerInfo.additionalNotEmptyCondition?.let { context.andand(it, loopCondition) } ?: loopCondition, newLoop)
            LoopReplacement(newLoop, notEmptyCheck)
        }
}

private class InitializerCallReplacer(symbolRemapper: SymbolRemapper, typeRemapper: TypeRemapper, val replacementCall: IrCall) :
    DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
    var initializerCall: IrCall? = null

    override fun visitCall(expression: IrCall): IrCall {
        if (initializerCall == null) {
            initializerCall = expression
            return replacementCall
        } else {
            throw IllegalStateException(
                "Multiple initializer calls found. First: ${initializerCall!!.render()}\nSecond: ${expression.render()}"
            )
        }
    }
}

internal class IndexedGetLoopHeader(
    headerInfo: IndexedGetHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<IndexedGetHeaderInfo>(headerInfo, builder, context, isLastInclusive = false) {

    override val loopInitStatements =
        listOfNotNull(headerInfo.objectVariable, inductionVariable, lastVariableIfCanCacheLast, stepVariable)

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder
    ) =
        with(builder) {
            // loopVariable = objectVariable[inductionVariable]
            val indexedGetFun = with(headerInfo.expressionHandler) { headerInfo.objectVariable.type.getFunction }
            val get = irCall(indexedGetFun.symbol).apply {
                dispatchReceiver = irGet(headerInfo.objectVariable)
                putValueArgument(0, irGet(inductionVariable))
            }
            // The call could be wrapped in an IMPLICIT_NOTNULL type-cast (see comment in ForLoopsLowering.gatherLoopVariableInfo()).
            // Find and replace the call to preserve any type-casts.
            loopVariable?.initializer = loopVariable?.initializer?.deepCopyWithSymbols { symbolRemapper, typeRemapper ->
                InitializerCallReplacer(symbolRemapper, typeRemapper, get)
            }
            // Even if there is no loop variable, we always want to call `get()` as it may have side-effects.
            // The un-lowered loop always calls `get()` on each iteration.
            listOf(loopVariable ?: get) + incrementInductionVariable(this)
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        // Loop is lowered into something like:
        //
        //   var inductionVar = 0
        //   var last = objectVariable.size
        //   while (inductionVar < last) {
        //       val loopVar = objectVariable.get(inductionVar)
        //       inductionVar++
        //       // Loop body
        //   }
        val newLoop = IrWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
            label = oldLoop.label
            condition = buildLoopCondition(this@with)
            body = newBody
        }
        LoopReplacement(newLoop, newLoop)
    }
}

internal class WithIndexLoopHeader(
    headerInfo: WithIndexHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : ForLoopHeader {

    private val nestedLoopHeader: ForLoopHeader
    private val indexVariable: IrVariable
    private val ownsIndexVariable: Boolean
    private val incrementIndexStatement: IrStatement?

    init {
        with(builder) {
            // To build the optimized/lowered `for` loop over a `withIndex()` call, we first need the header for the underlying iterable so
            // so that we know how to build the loop for that iterable. More info in comments in initializeIteration().
            nestedLoopHeader = when (val nestedInfo = headerInfo.nestedInfo) {
                is IndexedGetHeaderInfo -> IndexedGetLoopHeader(nestedInfo, this@with, context)
                is ProgressionHeaderInfo -> ProgressionLoopHeader(nestedInfo, this@with, context)
                is IterableHeaderInfo -> IterableLoopHeader(nestedInfo)
                is WithIndexHeaderInfo -> throw IllegalStateException("Nested WithIndexHeaderInfo not allowed for WithIndexLoopHeader")
                is FloatingPointRangeHeaderInfo, is ComparableRangeInfo -> error("Unexpected ${nestedInfo::class.simpleName} for loops")
            }

            // Do not build own indexVariable if the nested loop header has an inductionVariable == 0 and step == 1.
            // This is the case when the underlying iterable is an array, CharSequence, or a progression from 0 with step 1.
            // We can use the induction variable from the underlying iterable as the index variable, since it progresses in the same way.
            if (nestedLoopHeader is NumericForLoopHeader<*> &&
                nestedLoopHeader.inductionVariable.type.isInt() &&
                nestedLoopHeader.inductionVariable.initializer?.constLongValue == 0L &&
                nestedLoopHeader.stepExpression.constLongValue == 1L
            ) {
                indexVariable = nestedLoopHeader.inductionVariable
                ownsIndexVariable = false
                incrementIndexStatement = null
            } else {
                indexVariable = scope.createTmpVariable(
                    irInt(0),
                    nameHint = "index",
                    isMutable = true
                )
                ownsIndexVariable = true
                // `index++` during iteration initialization
                // TODO: KT-34665: Check for overflow for Iterable and Sequence (call to checkIndexOverflow()).
                val plusFun = indexVariable.type.getClass()!!.functions.first {
                    it.name == OperatorNameConventions.PLUS &&
                            it.valueParameters.size == 1 &&
                            it.valueParameters[0].type.isInt()
                }
                incrementIndexStatement =
                    irSet(
                        indexVariable.symbol, irCallOp(
                            plusFun.symbol, plusFun.returnType,
                            irGet(indexVariable),
                            irInt(1)
                        )
                    )
            }
        }
    }

    // Add the index variable (if owned) to the statements from the nested loop header.
    override val loopInitStatements = nestedLoopHeader.loopInitStatements.let { if (ownsIndexVariable) it + indexVariable else it }

    override val consumesLoopVariableComponents = true

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder
    ) =
        with(builder) {
            // The `withIndex()` extension function returns a lazy Iterable that wraps each element of the underlying iterable (e.g., array,
            // progression, Iterable, Sequence, CharSequence) into an IndexedValue containing the index of that element and the element
            // itself. The iterator for this lazy Iterable looks like this:
            //
            //   internal class IndexingIterator<out T>(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
            //     private var index = 0
            //     override fun hasNext() = iterator.hasNext()
            //     override fun next() = IndexedValue(checkIndexOverflow(index++), iterator.next())
            //   }
            //
            // IndexedValue looks like this:
            //
            //   data class IndexedValue<out T>(val index: Int, val value: T)
            //
            // For example, if the `for` loop is:
            //
            //   for ((i, v) in (1..10 step 2).withIndex()) { /* Loop body */ }
            //
            // ...the optimized loop for the underlying progression looks something like this:
            //
            //   var inductionVar = 1
            //   val last = 10
            //   val step = 2
            //   if (inductionVar <= last) {
            //     do {
            //       val v = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            //
            // ...and the optimized loop with `withIndex()` looks something like this (see "// ADDED" statements):
            //
            //   var inductionVar = 1
            //   val last = 10
            //   val step = 2
            //   var index = 0   // ADDED
            //   if (inductionVar <= last) {
            //     do {
            //       val i = index   // ADDED
            //       checkIndexOverflow(index++)   // ADDED
            //       val v = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            //
            // As another example, in a for-loop over a call to `Iterable<*>.withIndex()` or `Sequence<*>.withIndex()`, e.g.:
            //
            //   for ((i, v) in listOf(2, 3, 5, 7, 11).withIndex()) { /* Loop body */ }
            //
            // For-loops over an Iterable are normally not optimized, but when getting the underlying iterable for `withIndex()` (and ONLY
            // in this case), we use DefaultIterableHandler to match it and IterableLoopHeader to build the underlying loop. The optimized
            // loop with `withIndex()` looks something like this:
            //
            //   val iterator = listOf(2, 3, 5, 7, 11).iterator()
            //   var index = 0
            //   while (it.hasNext())
            //     val i = index
            //     checkIndexOverflow(index++)
            //     val v = it.next()
            //     // Loop body
            //   }
            //
            // We "wire" the 1st destructured component to index, and the 2nd to the loop variable value from the underlying iterable.
            loopVariableComponents[1]?.initializer = irGet(indexVariable)
            listOfNotNull(loopVariableComponents[1], incrementIndexStatement) + nestedLoopHeader.initializeIteration(
                loopVariableComponents[2],
                linkedMapOf(),
                builder
            )
        }

    // Use the nested loop header to build the loop. More info in comments in initializeIteration().
    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        nestedLoopHeader.buildLoop(builder, oldLoop, newBody)
}

internal class IterableLoopHeader(
    private val headerInfo: IterableHeaderInfo
) : ForLoopHeader {
    override val loopInitStatements = listOf(headerInfo.iteratorVariable)

    override val consumesLoopVariableComponents = false

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder
    ): List<IrStatement> =
        with(builder) {
            // loopVariable = iteratorVar.next()
            val iteratorClass = headerInfo.iteratorVariable.type.getClass()!!
            val next =
                irCall(iteratorClass.functions.first {
                    it.name == OperatorNameConventions.NEXT && it.valueParameters.isEmpty()
                }.symbol).apply {
                    dispatchReceiver = irGet(headerInfo.iteratorVariable)
                }
            // The call could be wrapped in an IMPLICIT_NOTNULL type-cast (see comment in ForLoopsLowering.gatherLoopVariableInfo()).
            // Find and replace the call to preserve any type-casts.
            loopVariable?.initializer = loopVariable?.initializer?.deepCopyWithSymbols { symbolRemapper, typeRemapper ->
                InitializerCallReplacer(symbolRemapper, typeRemapper, next)
            }
            // Even if there is no loop variable, we always want to call `next()` for iterables and sequences.
            listOf(loopVariable ?: next.coerceToUnitIfNeeded(next.type, context.irBuiltIns))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        // Loop is lowered into something like:
        //
        //   var iteratorVar = someIterable.iterator()
        //   while (iteratorVar.hasNext()) {
        //       val loopVar = iteratorVar.next()
        //       // Loop body
        //   }
        val iteratorClass = headerInfo.iteratorVariable.type.getClass()!!
        val hasNext =
            irCall(iteratorClass.functions.first { it.name == OperatorNameConventions.HAS_NEXT && it.valueParameters.isEmpty() }).apply {
                dispatchReceiver = irGet(headerInfo.iteratorVariable)
            }
        val newLoop = IrWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
            label = oldLoop.label
            condition = hasNext
            body = newBody
        }
        LoopReplacement(newLoop, newLoop)
    }
}

/**
 * Given the for-loop iterator variable, extract information about the iterable subject
 * and create a [ForLoopHeader] from it.
 */
internal class HeaderProcessor(
    private val context: CommonBackendContext,
    private val headerInfoBuilder: HeaderInfoBuilder,
    private val scopeOwnerSymbol: () -> IrSymbol
) {

    private val symbols = context.ir.symbols

    /**
     * Extracts information for building the for-loop (as a [ForLoopHeader]) from the given
     * "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    fun extractHeader(variable: IrVariable): ForLoopHeader? {
        // Verify the variable type is a subtype of Iterator<*>.
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        if (!variable.type.isSubtypeOfClass(symbols.iterator)) {
            return null
        }

        // Get the iterable expression, e.g., `someIterable` in the following loop variable declaration:
        //
        //   val it = someIterable.iterator()
        val iteratorCall = variable.initializer as? IrCall
        val iterable = iteratorCall?.run {
            if (extensionReceiver != null) {
                extensionReceiver
            } else {
                dispatchReceiver
            }
        }

        // Collect loop information from the iterable expression.
        val headerInfo = iterable?.accept(headerInfoBuilder, iteratorCall)
            ?: return null  // If the iterable is not supported.

        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        return when (headerInfo) {
            is IndexedGetHeaderInfo -> IndexedGetLoopHeader(headerInfo, builder, context)
            is ProgressionHeaderInfo -> ProgressionLoopHeader(headerInfo, builder, context)
            is WithIndexHeaderInfo -> WithIndexLoopHeader(headerInfo, builder, context)
            is IterableHeaderInfo -> IterableLoopHeader(headerInfo)
            is FloatingPointRangeHeaderInfo, is ComparableRangeInfo -> error("Unexpected ${headerInfo::class.simpleName} for loops")
        }
    }
}
