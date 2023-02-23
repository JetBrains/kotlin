/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import kotlin.reflect.KProperty

open class AbstractSupertypesTraverser(
    type: SimpleTypeMarker,
    private val state: TypeCheckerState,
) {
    init {
        state.prepare(type)
    }

    enum class CalculationState {
        CALCULATING, DONE,
    }

    class DataCalculator<T>(
        var data: T,
        var currentState: CalculationState,
        val calculate: (T, SimpleTypeMarker) -> Pair<T, CalculationState>,
    ) {
        override fun toString() = "${currentState.name}: $data"
    }

    private fun <T> DataCalculator<T>.recalculate(type: SimpleTypeMarker): CalculationState {
        val (newData, newCurrentState) = calculate(data, type)
        currentState = newCurrentState
        data = newData
        return newCurrentState
    }

    private val unfinishedDataCalculators = mutableListOf<DataCalculator<*>>()

    private fun recalculateDataUntilReady(
        calculator: DataCalculator<*>,
        type: SimpleTypeMarker,
    ): TypeCheckerState.SupertypesAction {
        var allFinished = true
        val iterator = unfinishedDataCalculators.iterator()

        while (iterator.hasNext()) {
            val it = iterator.next()
            val state = it.recalculate(type)

            if (state == CalculationState.DONE) {
                iterator.remove()
            }

            allFinished = allFinished && state == CalculationState.DONE
        }

        val indexFinished = calculator.currentState == CalculationState.DONE

        return when {
            allFinished -> TypeCheckerState.SupertypesAction.STOP
            indexFinished -> TypeCheckerState.SupertypesAction.PAUSE
            else -> TypeCheckerState.SupertypesAction.NEXT
        }
    }

    private fun traverseSupertypesUntilReady(calculator: DataCalculator<*>) {
        state.traverseFurtherSupertypes(
            action = { recalculateDataUntilReady(calculator, it) },
            supertypesPolicy = { TypeCheckerState.SupertypesPolicy.LowerIfFlexible },
        )
    }

    inner class DataAccessor<T>(private val calculator: DataCalculator<T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (calculator.currentState == CalculationState.CALCULATING) {
                traverseSupertypesUntilReady(calculator)
            }

            return calculator.data
        }
    }

    fun <T> lazyTraverse(initialValue: T, fold: (T, SimpleTypeMarker) -> Pair<T, CalculationState>): DataAccessor<T> {
        val calculator = DataCalculator(initialValue, CalculationState.CALCULATING, fold)
        unfinishedDataCalculators.add(calculator)
        return DataAccessor(calculator)
    }
}

fun continueTraversalIf(condition: Boolean) = when (condition) {
    true -> AbstractSupertypesTraverser.CalculationState.CALCULATING
    false -> AbstractSupertypesTraverser.CalculationState.DONE
}
