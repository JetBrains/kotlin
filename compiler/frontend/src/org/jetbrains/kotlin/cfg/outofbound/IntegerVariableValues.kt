/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cfg.outofbound

import com.google.common.collect.UnmodifiableListIterator
import org.jetbrains.kotlin.cfg.outofbound.ValuesData
import org.jetbrains.kotlin.cfg.outofbound.BooleanVariableValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

// Represents possible integer variable values
public class IntegerVariableValues() {
    // if true - no values assigned to variable (variable is defined but not initialized)
    public var isEmpty: Boolean = true
        private set
    // if true - analysis can't define variable values
    public var isUndefined: Boolean = false
        private set
    private val availableValues: MutableSet<Int> = HashSet()
    private val unavailableValues: MutableSet<UnavailableValue> = HashSet()

    public val isDefined: Boolean
        get() = !(isEmpty || isUndefined || availableValues.isEmpty())
    private val allValuesNumber: Int
        get() = availableValues.size() + unavailableValues.size()
    // this constant is chosen randomly
    private val undefinedThreshold = 20

    constructor(value: Int) : this() {
        this.addAvailable(value)
    }

    public fun setUndefined() {
        isEmpty = false
        isUndefined = true
        availableValues.clear()
        unavailableValues.clear()
    }

    public fun addAll(values: IntegerVariableValues) {
        values.availableValues.forEach { addAvailable(it) }
        values.unavailableValues.forEach { addUnavailable(it) }
    }

    private fun addAvailable(value: Int) {
        if (isUndefined) {
            return
        }
        availableValues.add(value)
        isEmpty = false
        if (allValuesNumber == undefinedThreshold) {
            setUndefined()
        }
    }

    private fun addUnavailable(value: UnavailableValue) {
        if (isUndefined) {
            return
        }
        unavailableValues.add(value)
        isEmpty = false
        if (allValuesNumber == undefinedThreshold) {
            setUndefined()
        }
    }

    public fun copy(): IntegerVariableValues {
        val copy = IntegerVariableValues()
        copy.isEmpty = isEmpty
        copy.isUndefined = isUndefined
        copy.availableValues.addAll(availableValues)
        copy.unavailableValues.addAll(unavailableValues)
        return copy
    }

    public fun getAvailableValues(): List<Int> =
            Collections.unmodifiableList(availableValues.toList())

    public fun makeAllValuesUnavailable(sinceLexicalScope: LexicalScope) {
        availableValues.forEach { unavailableValues.add(UnavailableValue(it, sinceLexicalScope)) }
        availableValues.clear()
    }

    public fun leaveOnlyPassedValuesAvailable(valuesToLeaveAvailable: Set<Int>, sinceLexicalScope: LexicalScope) {
        val currentAvailableValues = LinkedList<Int>()
        currentAvailableValues.addAll(availableValues)
        currentAvailableValues.forEach {
            if (!valuesToLeaveAvailable.contains(it)) {
                availableValues.remove(it)
                unavailableValues.add(UnavailableValue(it, sinceLexicalScope))
            }
        }
    }

    public fun tryMakeValuesAvailable(lexicalScope: LexicalScope) {
        val currentUnavailableValues = LinkedList<UnavailableValue>()
        currentUnavailableValues.addAll(unavailableValues)
        currentUnavailableValues.forEach {
            if (it.sinceLexicalScope.depth > lexicalScope.depth) {
                unavailableValues.remove(it)
                availableValues.add(it.value)
            }
        }
    }

    // operators overloading
    public fun plus(others: IntegerVariableValues): IntegerVariableValues =
        applyEachToEach(others) { x, y -> x + y }
    public fun minus(others: IntegerVariableValues): IntegerVariableValues =
        applyEachToEach(others) { x, y -> x - y }
    public fun times(others: IntegerVariableValues): IntegerVariableValues =
        applyEachToEach(others) { x, y -> x * y }
    public fun div(others: IntegerVariableValues): IntegerVariableValues =
        applyEachToEach(others) { x, y ->
            if (y == 0) {
                throw Exception("IntegerVariableValues - division by zero detected")
            }
            else {
                x / y
            }
        }
    public fun rangeTo(others: IntegerVariableValues): IntegerVariableValues {
        if (this.isDefined && others.isDefined) {
            val minOfLeftOperand = availableValues.min() as Int
            val maxOfRightOperand = others.availableValues.max() as Int
            val rangeValues = IntegerVariableValues()
            for (value in minOfLeftOperand..maxOfRightOperand) {
                rangeValues.addAvailable(value)
            }
            return rangeValues
        }
        return createUndefined()
    }

    private fun applyEachToEach(others: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues {
        if (this.isDefined && others.isDefined) {
            val results = availableValues.map { leftOp ->
                others.availableValues.map { rightOp -> operation(leftOp, rightOp) }
            }.flatten()
            return createFromCollection(results)
        }
        return createUndefined()
    }

    // special operators, (IntegerVariableValues, IntegerVariableValues) -> BoolVariableValue
    public fun less(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue {
        if (!this.isDefined) {
            return BooleanVariableValue.undefinedWithNoRestrictions
        }
        if (!other.isDefined) {
            return undefinedWithFullRestrictions(valuesData)
        }
        if (other.availableValues.size() > 1) {
            // this check means that in expression "x < y" only one element set is supported for "y"
            return undefinedWithFullRestrictions(valuesData)
        }
        val otherValue = other.availableValues.single()
        return if (availableValues.size() == 1) {
            val thisValue = availableValues.first()
            when {
                thisValue < otherValue -> BooleanVariableValue.True
                else -> BooleanVariableValue.False
            }
        }
        else {
            val thisArray = availableValues.toIntArray()
            thisArray.sort()
            when {
                thisArray.last() < otherValue -> BooleanVariableValue.True
                thisArray.last() == otherValue -> {
                    thisVarDescriptor?.let {
                        val withoutLast = thisArray.copyOf(thisArray.size() - 1).toSet()
                        val onTrueRestrictions = mapOf(it to withoutLast)
                        val onFalseRestrictions = mapOf(it to setOf(thisArray.last()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                    ?: undefinedWithFullRestrictions(valuesData)
                }
                thisArray.last() > otherValue && otherValue > thisArray.first() -> {
                    thisVarDescriptor?.let {
                        val bound = thisArray.indexOfFirst { it >= otherValue }
                        val lessValuesInThis = thisArray.copyOfRange(0, bound).toSet()
                        val greaterOrEqValuesInThis = thisArray.copyOfRange(bound, thisArray.size()).toSet()
                        val onTrueRestrictions = mapOf(it to lessValuesInThis)
                        val onFalseRestrictions = mapOf(it to greaterOrEqValuesInThis)
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                    ?: undefinedWithFullRestrictions(valuesData)
                }
                thisArray.first() == otherValue -> {
                    thisVarDescriptor?.let {
                        val withoutFirst = thisArray.copyOfRange(1, thisArray.size()).toSet()
                        val onTrueRestrictions = mapOf(it to withoutFirst)
                        val onFalseRestrictions = mapOf(it to setOf(thisArray.first()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                    ?: undefinedWithFullRestrictions(valuesData)
                }
                else -> BooleanVariableValue.False
            }
        }
    }

    private fun undefinedWithFullRestrictions(valuesData: ValuesData): BooleanVariableValue.Undefined {
        val restrictions = valuesData.intVarsToValues.keySet()
                .map { Pair(it, setOf<Int>()) }
                .toMap()
        return BooleanVariableValue.Undefined(restrictions, restrictions)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntegerVariableValues) {
            return false
        }
        return isEmpty == other.isEmpty
               && isUndefined == other.isUndefined
               && availableValues == other.availableValues
               && unavailableValues == other.unavailableValues
    }

    override fun hashCode(): Int {
        var code = 7
        code = 31 * code + isEmpty.hashCode()
        code = 31 * code + isUndefined.hashCode()
        code = 31 * code + availableValues.hashCode()
        code = 31 * code + unavailableValues.hashCode()
        return code
    }

    override fun toString(): String {
        return when {
            isEmpty -> "-"
            isUndefined -> "?"
            else -> "${availableValues.toSortedList().toString()}${unavailableValues.toSortedListBy { it.value }.toString()}"
        }
    }

    companion object {
        public fun createUndefined(): IntegerVariableValues {
            val values = IntegerVariableValues()
            values.setUndefined()
            return values
        }
        public fun createFromCollection(collection: Collection<Int>): IntegerVariableValues {
            val values = IntegerVariableValues()
            collection.forEach { values.addAvailable(it) }
            return values
        }
    }

    private data class UnavailableValue(val value: Int, val sinceLexicalScope: LexicalScope) {
        override fun toString(): String = "$value(${sinceLexicalScope.depth})"
    }
}
