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
import com.intellij.psi.tree.IElementType
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
    private val values: MutableSet<Int> = HashSet()

    public val isDefined: Boolean
        get() = !(isEmpty || isUndefined)
    // this constant is chosen randomly
    private val undefinedThreshold = 20

    constructor(value: Int) : this() {
        this.add(value)
    }

    public fun setUndefined() {
        isEmpty = false
        isUndefined = true
        values.clear()
    }

    public fun addAll(values: IntegerVariableValues) {
        values.values.forEach { add(it) }
    }

    private fun add(value: Int) {
        if (isUndefined) {
            return
        }
        values.add(value)
        isEmpty = false
        if (values.size() == undefinedThreshold) {
            setUndefined()
        }
    }

    public fun copy(): IntegerVariableValues {
        val copy = IntegerVariableValues()
        copy.isEmpty = isEmpty
        copy.isUndefined = isUndefined
        copy.values.addAll(values)
        return copy
    }

    public fun getValues(): List<Int> =
            Collections.unmodifiableList(values.toList())

    public fun leaveOnlyValuesInSet(valuesToLeave: Set<Int>, sinceLexicalScope: LexicalScope) {
        val currentAvailableValues = LinkedList<Int>()
        currentAvailableValues.addAll(values)
        currentAvailableValues.forEach {
            if (!valuesToLeave.contains(it)) {
                values.remove(it)
            }
        }
        if (values.isEmpty()) {
            setUndefined()
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
        applyEachToEach(
                createFromCollection(others.values.filter { it != 0 }),
                { x, y -> x / y }
        )
    public fun rangeTo(others: IntegerVariableValues): IntegerVariableValues {
        if (this.isDefined && others.isDefined) {
            val minOfLeftOperand = values.min() as Int
            val maxOfRightOperand = others.values.max() as Int
            val rangeValues = IntegerVariableValues()
            for (value in minOfLeftOperand..maxOfRightOperand) {
                rangeValues.add(value)
            }
            return rangeValues
        }
        return createUndefined()
    }

    private fun applyEachToEach(others: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues {
        if (this.isDefined && others.isDefined) {
            val results = values.map { leftOp ->
                others.values.map { rightOp -> operation(leftOp, rightOp) }
            }.flatten()
            return createFromCollection(results)
        }
        return createUndefined()
    }

    public fun minus(): IntegerVariableValues =
            createFromCollection(values.map { -1 * it })

    // special operators, (IntegerVariableValues, IntegerVariableValues) -> BoolVariableValue
    public fun lessThan(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue.Undefined =
            comparison(other, thisVarDescriptor, valuesData,
                       { array, value -> array.indexOfFirst { it >= value } },
                       { varDescriptor, valuesWithLessIndices, valuesWithGreaterOrEqIndices ->
                           mapOf(varDescriptor to valuesWithLessIndices) to mapOf(varDescriptor to valuesWithGreaterOrEqIndices)
                       }
            )

    public fun greaterThan(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue.Undefined =
            comparison(other, thisVarDescriptor, valuesData,
                       { array, value -> array.indexOfFirst { it > value } },
                       { varDescriptor, valuesWithLessIndices, valuesWithGreaterOrEqIndices ->
                           mapOf(varDescriptor to valuesWithGreaterOrEqIndices) to mapOf(varDescriptor to valuesWithLessIndices)
                       }
            )

    public fun greaterOrEq(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue.Undefined {
        val lessThenRes = lessThan(other, thisVarDescriptor, valuesData)
        return BooleanVariableValue.Undefined(lessThenRes.onFalseRestrictions, lessThenRes.onTrueRestrictions)
    }

    public fun lessOrEq(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue.Undefined {
        val greaterThenRes = greaterThan(other, thisVarDescriptor, valuesData)
        return BooleanVariableValue.Undefined(greaterThenRes.onFalseRestrictions, greaterThenRes.onTrueRestrictions)
    }

    private fun comparison(
            other: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData,
            findIndex: (IntArray, Int) -> Int,
            createRestrictions: (VariableDescriptor, Set<Int>, Set<Int>) -> Pair<Map<VariableDescriptor, Set<Int>>, Map<VariableDescriptor, Set<Int>>>
    ): BooleanVariableValue.Undefined {
        if (!this.isDefined) {
            return BooleanVariableValue.undefinedWithNoRestrictions
        }
        if (!other.isDefined || other.values.size() > 1) {
            // the second check means that in expression "x 'operator' y" only one element set is supported for "y"
            return undefinedWithFullRestrictions(valuesData)
        }
        val otherValue = other.values.single()
        val thisArray = values.toIntArray()
        thisArray.sort()
        return thisVarDescriptor?.let {
            val foundIndex = findIndex(thisArray, otherValue)
            val bound = if (foundIndex < 0) thisArray.size() else foundIndex
            val valuesWithLessIndices = thisArray.copyOfRange(0, bound).toSet()
            val valuesWithGreaterOrEqIndices = thisArray.copyOfRange(bound, thisArray.size()).toSet()
            val (onTrueRestrictions, onFalseRestrictions) = createRestrictions(it, valuesWithLessIndices, valuesWithGreaterOrEqIndices)
            BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
        } ?: undefinedWithFullRestrictions(valuesData)
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
               && values == other.values
    }

    override fun hashCode(): Int {
        var code = 7
        code = 31 * code + isEmpty.hashCode()
        code = 31 * code + isUndefined.hashCode()
        code = 31 * code + values.hashCode()
        return code
    }

    override fun toString(): String {
        return when {
            isEmpty -> "-"
            isUndefined -> "?"
            else -> "${values.toSortedList().toString()}"
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
            collection.forEach { values.add(it) }
            return values
        }
    }
}
