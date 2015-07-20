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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

// Represents possible integer variable values
public class IntegerVariableValues private constructor() {
    // if true - no values assigned to variable (variable is defined but not initialized)
    public var areEmpty: Boolean = true
        private set
    // if true - analysis can't define variable values
    public var cantBeDefined: Boolean = false
        private set
    public val areDefined: Boolean
        get() = !(areEmpty || cantBeDefined)

    private val values: MutableSet<Int> = HashSet()
    // this constant is chosen randomly
    private val undefinedThreshold = 20

    public fun setUndefined() {
        areEmpty = false
        cantBeDefined = true
        values.clear()
    }
    public fun add(value: Int) {
        if(cantBeDefined) {
            return
        }
        values.add(value)
        areEmpty = false
        if(values.size().equals(undefinedThreshold)) {
            setUndefined()
        }
    }
    public fun addAll(values: Collection<Int>) {
        for(value in values) {
            add(value)
        }
    }
    public fun addAll(values: IntegerVariableValues) {
        for(value in values.values) {
            add(value)
        }
    }
    public fun getValues(): Set<Int> = Collections.unmodifiableSet(values)

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
            } else {
                x / y
            }
        }
    public fun rangeTo(others: IntegerVariableValues): IntegerVariableValues {
        if(this.areDefined && others.areDefined) {
            // we can safely use casts below because of areDefined checks above
            val minOfLeftOperand = values.min() as Int
            val maxOfRightOperand = others.values.max() as Int
            val rangeValues = IntegerVariableValues.empty()
            for (value in minOfLeftOperand .. maxOfRightOperand) {
                rangeValues.add(value)
            }
            return rangeValues
        }
        return IntegerVariableValues.cantBeDefined()
    }

    private fun applyEachToEach(others: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues {
        if(this.areDefined && others.areDefined) {
            val resultSet = HashSet<Int>()
            for (leftOp in this.values) {
                for (rightOp in others.values) {
                    resultSet.add(operation(leftOp, rightOp))
                }
            }
            return IntegerVariableValues.ofCollection(resultSet)
        }
        return IntegerVariableValues.cantBeDefined()
    }

    // special operators, (IntegerVariableValues, IntegerVariableValues) -> BoolVariableValue
    public fun less(
            others: IntegerVariableValues,
            thisVarDescriptor: VariableDescriptor?,
            valuesData: ValuesData
    ): BooleanVariableValue {
        if(!this.areDefined) {
            return BooleanVariableValue.undefinedWithNoRestrictions
        }
        if(!others.areDefined) {
            return undefinedWithFullRestrictions(valuesData)
        }
        if(others.values.size() > 1) {
            // this check means that in expression "x < y" only one element set is supported for "y"
            return undefinedWithFullRestrictions(valuesData)
        }
        val otherValue = others.values.single()
        return if(values.size() == 1) {
            val thisValue = values.toIntArray().first()
            when {
                thisValue < otherValue -> BooleanVariableValue.True
                else -> BooleanVariableValue.False
            }
        } else {
            val thisArray = values.toIntArray()
            thisArray.sort()
            when {
                thisArray.last() < otherValue -> BooleanVariableValue.True
                thisArray.last() == otherValue -> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val withoutLast = thisArray.copyOf(thisArray.size() - 1).toArrayList()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.ofCollection(withoutLast))
                        val onFalseRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.singleton(thisArray.last()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                }
                thisArray.last() > otherValue && otherValue > thisArray.first()-> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val bound = thisArray.indexOfFirst { it >= otherValue }
                        val lessValuesInThis = thisArray.copyOfRange(0, bound).toArrayList()
                        val greaterOrEqValuesInThis = thisArray.copyOfRange(bound, thisArray.size()).toArrayList()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.ofCollection(lessValuesInThis))
                        val onFalseRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.ofCollection(greaterOrEqValuesInThis))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                }
                thisArray.first() == otherValue -> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val withoutFirst = thisArray.copyOfRange(1, thisArray.size()).toArrayList()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.ofCollection(withoutFirst))
                        val onFalseRestrictions = mapOf(thisVarDescriptor to IntegerVariableValues.singleton(thisArray.first()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                }
                else -> BooleanVariableValue.False
            }
        }
    }

    private fun undefinedWithFullRestrictions(valuesData: ValuesData): BooleanVariableValue.Undefined {
        val restrictions = valuesData.intVarsToValues.keySet()
                .map { Pair(it, IntegerVariableValues.empty()) }
                .toMap()
        return BooleanVariableValue.Undefined(restrictions, restrictions)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is IntegerVariableValues) {
            return false
        }
        return areEmpty.equals(other.areEmpty)
               && cantBeDefined.equals(other.cantBeDefined)
               && values.equals(other.values)
    }
    override fun hashCode(): Int {
        var code = 7
        code = 31 * code + areEmpty.hashCode()
        code = 31 * code + cantBeDefined.hashCode()
        code = 31 * code + values.hashCode()
        return code
    }
    override fun toString(): String = values.toString()

    companion object {
        public fun empty(): IntegerVariableValues = IntegerVariableValues()
        public fun singleton(value: Int): IntegerVariableValues {
            val values = empty()
            values.add(value)
            return values
        }
        public fun cantBeDefined(): IntegerVariableValues {
            val values = empty()
            values.setUndefined()
            return values
        }
        public fun ofCollection(collection: Collection<Int>): IntegerVariableValues {
            val values = empty()
            values.addAll(collection)
            return values
        }
    }
}
