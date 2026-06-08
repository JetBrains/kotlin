/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import kotlin.collections.get

internal class SequenceData(
    val mapReplacement: MapReplacement,
    val sequenceSource: SequenceSource,
    val newLoopPrologue: LoopPrologue,
    val declarationsBeforeLoop: PreLoopDeclarations,
) {
    // mapReplacement for a given sequence expression stores a composition of functions applied to the base sequence via `map`
    internal fun interface MapReplacement {
        operator fun invoke(
            builderWithParent: IrBuilderWithParent,
            expression: IrExpression
        ): IrExpression
    }

    internal fun interface LoopPrologue {
        operator fun invoke(
            builder: IrBuilderWithParent,
            loop: IrLoop,
            expression: IrExpression,
            transformBody: (IrExpression) -> IrExpression
        ): IrExpression
    }

    internal fun interface PreLoopDeclarations {
        operator fun invoke(
            builder: IrBuilderWithScope
        ): MutableList<IrVariable>
    }

    internal fun createMapReplacement(function: IrRichFunctionReference): MapReplacement =
        { builderWithParent: IrBuilderWithParent, argument: IrExpression ->
            builderWithParent.first.callRichFunctionReference(function, builderWithParent.second, argument)
        }

    internal fun createMapReplacement(function: IrRichFunctionReference, index: IrExpression): MapReplacement =
        { builderWithParent: IrBuilderWithParent, argument: IrExpression ->
            builderWithParent.first.callRichFunctionReference(function, builderWithParent.second, index, argument)
        }

    internal fun applyMap(newMapReplacement: MapReplacement): SequenceData {
        return SequenceData(
            composeMapReplacements(this.mapReplacement, newMapReplacement),
            this.sequenceSource,
            this.newLoopPrologue,
            this.declarationsBeforeLoop,
        )
    }

    private fun composeMapReplacements(
        accumulator: MapReplacement,
        newFunction: MapReplacement,
    ): MapReplacement = { builder, argument -> newFunction(builder, accumulator(builder, argument)) }

    /**
     * Filter replacement is constructed like this:
     * ```
     *    { initialValue, expressionDependentOnValue ->
     *        val value1 = firstMapReplacement(initialValue)
     *       val isNotFiltered1 = firstFilter(value1)
     *       if (isNotFiltered1) {
     *           val value2 = secondMapReplacement(value1)
     *           val isNotFiltered2 = secondFilter(value2)
     *           if (isNotFiltered2) {
     *               ... {
     *                   expressionDependentOnValue(finalValue)
     *               }
     *           }
     *        }
     *    }
     * ```
     */

    internal fun createNewFilterSegment(
        filterFunction: IrRichFunctionReference,
    ): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, builderWithParent.second, irGet(newValue)))
            +irIfThen(context.irBuiltIns.unitType, irGet(willStay), expressionDependentOnValue(irGet(newValue)))
        }
    }

    internal fun createNewFilterNotSegment(
        filterFunction: IrRichFunctionReference,
    ): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(callRichFunctionReference(filterFunction, builderWithParent.second, irGet(newValue)))
            +irIfThen(context.irBuiltIns.unitType, irNot(irGet(willStay)), expressionDependentOnValue(irGet(newValue)))
        }
    }

    internal fun createNewFilterNotNullSegment(): LoopPrologue = { builderWithParent, _, valueGenerator, expressionDependentOnValue ->
        builderWithParent.first.irBlock {
            val newValue = irTemporary(mapReplacement(builderWithParent, valueGenerator))
            val willStay = irTemporary(irNot(irEquals(irGet(newValue), irNull())))
            +irIfThen(context.irBuiltIns.unitType, irGet(willStay), expressionDependentOnValue(irGet(newValue)))
        }
    }

    private fun composeFilterReplacements(accumulator: LoopPrologue, nextSegment: LoopPrologue): LoopPrologue =
        { builder, loop, valueGenerator, expressionDependentOnValue ->
            accumulator(builder, loop, valueGenerator) { nextValue -> nextSegment(builder, loop, nextValue, expressionDependentOnValue) }
        }

    internal fun applyFilter(
        newSegment: LoopPrologue,
    ): SequenceData {
        val newLoopPrologue = composeFilterReplacements(this@SequenceData.newLoopPrologue, newSegment)
        return SequenceData(
            defaultMapReplacement,
            sequenceSource,
            newLoopPrologue,
            declarationsBeforeLoop,
        )
    }

    /**
     * Take replacement has two parts: the take variable declaration and the actual place where we check if we have taken enough
     * ```
     * seq.someMapsAndFilters.take(n)
     * for (i in seq) {body(i)}
     * ```
     * becomes
     * ```
     * val takeCount = 0
     * while(...) {
     *     filterReplacement
     *     mapReplacement
     *     takeCount++
     *     if (takeCount > n) break
     *     body(mapReplacementValue)
     * }
     * ```
     * filterReplacement + mapReplacement is inside the loop already
     */

    private fun createNewTakeVariable(builder: IrBuilderWithScope): IrVariable {
        return builder.scope.createTemporaryVariable(
            builder.irInt(0),
            isMutable = true,
            nameHint = "takeVar"
        )
    }

    private fun createTakeReplacement(
        builderWithParent: IrBuilderWithParent,
        valueGenerator: IrExpression,
        expressionDependentOnValue: (IrExpression) -> IrExpression,
        getOrCreateTakeVariable: (IrBuilderWithScope) -> IrVariable,
        loop: IrLoop,
        takeArgument: IrExpression,
    ): IrExpression {
        val builder = builderWithParent.first
        val takeVariable = getOrCreateTakeVariable(builder)
        val classifier = takeVariable.type.classifierOrNull
        val lessThanSymbol = builder.context.irBuiltIns.lessFunByOperandType[classifier]
            ?: error("No lessThan function found for type ${takeVariable.type}")
        return builder.irBlock {
            val tmp = irTemporary(mapReplacement(builderWithParent, valueGenerator))

            // takeVariable++
            +irSet(takeVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
                dispatchReceiver = irGet(takeVariable)
                arguments[1] = irInt(1)
            })

            // if (takeVariable > takeArgument) break
            +irIfThen(
                builder.context.irBuiltIns.unitType,
                irCall(lessThanSymbol).apply {
                    arguments[0] = takeArgument.deepCopyWithSymbols(builderWithParent.second)
                    arguments[1] = irGet(takeVariable)
                },
                irBreak(loop)
            )
            +expressionDependentOnValue(irGet(tmp))
        }
    }

    fun applyTake(
        takeArgument: IrExpression,
    ): SequenceData {
        val takeVariableCell = object {
            var value: IrVariable? = null
        }
        val getOrCreateTakeVariable = { builder: IrBuilderWithScope ->
            takeVariableCell.value ?: createNewTakeVariable(builder).also {
                takeVariableCell.value = it
            }
        }

        val newFilterReplacement = composeFilterReplacements(
            this.newLoopPrologue
        ) { builderWithParent, loop, valueGenerator, expressionDependentOnValue ->
            createTakeReplacement(
                builderWithParent,
                valueGenerator,
                expressionDependentOnValue,
                getOrCreateTakeVariable,
                loop,
                takeArgument,
            )
        }

        val newTakeVariableDeclarations = { builder: IrBuilderWithScope ->
            val takeVariable = getOrCreateTakeVariable(builder)
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(takeVariable)
            declarations
        }

        return SequenceData(
            defaultMapReplacement,
            this.sequenceSource,
            newFilterReplacement,
            newTakeVariableDeclarations,
        )
    }

    internal fun addDeclaration(declaration: IrVariable): SequenceData {
        val newDeclarations = { builder: IrBuilderWithScope ->
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(declaration)
            declarations
        }
        return SequenceData(
            this.mapReplacement,
            this.sequenceSource,
            this.newLoopPrologue,
            newDeclarations,
        )
    }

    internal fun addDeclarationExpectingBuilder(declaration: (IrBuilderWithScope) -> IrVariable): SequenceData {
        val newDeclarations = { builder: IrBuilderWithScope ->
            val declarations = declarationsBeforeLoop(builder)
            declarations.add(declaration(builder))
            declarations
        }
        return SequenceData(
            this.mapReplacement,
            this.sequenceSource,
            this.newLoopPrologue,
            newDeclarations,
        )
    }

    companion object {
        val defaultMapReplacement: MapReplacement = { _, value -> value }
        val defaultLoopPrologue: LoopPrologue = { _, _, value, expressionExpectingValue -> expressionExpectingValue(value) }
        val defaultTakeVariableDeclarations: (IrBuilderWithScope) -> MutableList<IrVariable> =
            { _ -> mutableListOf() }
    }
}
