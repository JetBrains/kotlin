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
public class IntegerVariableValues private constructor() {
    // if true - no values assigned to variable (variable is defined but not initialized)
    public var areEmpty: Boolean = true
        private set
    // if true - analysis can't define variable values
    public var cantBeDefined: Boolean = false
        private set
    private val availableValues: MutableSet<Int> = HashSet()
    private val unavailableValues: MutableSet<UnavailableValue> = HashSet()

    public val areDefined: Boolean
        get() = !(areEmpty || cantBeDefined || availableValues.isEmpty())
    private val allValuesNumber: Int
        get() = availableValues.size() + unavailableValues.size()
    // this constant is chosen randomly
    private val cantBeDefinedThreshold = 20

    public fun setCantBeDefined() {
        areEmpty = false
        cantBeDefined = true
        availableValues.clear()
        unavailableValues.clear()
    }

    public fun addAll(values: IntegerVariableValues) {
        values.availableValues.forEach { addAvailable(it) }
        values.unavailableValues.forEach { addUnavailable(it) }
    }

    private fun addAvailable(value: Int) {
        if(cantBeDefined) {
            return
        }
        availableValues.add(value)
        areEmpty = false
        if(allValuesNumber == cantBeDefinedThreshold) {
            setCantBeDefined()
        }
    }

    private fun addUnavailable(value: UnavailableValue) {
        if(cantBeDefined) {
            return
        }
        unavailableValues.add(value)
        areEmpty = false
        if(allValuesNumber == cantBeDefinedThreshold) {
            setCantBeDefined()
        }
    }

    public fun copy(): IntegerVariableValues {
        val copy = IntegerVariableValues()
        copy.areEmpty = areEmpty
        copy.cantBeDefined = cantBeDefined
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
            if(!valuesToLeaveAvailable.contains(it)) {
                availableValues.remove(it)
                unavailableValues.add(UnavailableValue(it, sinceLexicalScope))
            }
        }
    }

    public fun tryMakeValuesAvailable(lexicalScope: LexicalScope) {
        val currentUnavailableValues = LinkedList<UnavailableValue>()
        currentUnavailableValues.addAll(unavailableValues)
        currentUnavailableValues.forEach {
            if(it.sinceLexicalScope.depth > lexicalScope.depth) {
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
            } else {
                x / y
            }
        }
    public fun rangeTo(others: IntegerVariableValues): IntegerVariableValues {
        if(this.areDefined && others.areDefined) {
            val minOfLeftOperand = availableValues.min() as Int
            val maxOfRightOperand = others.availableValues.max() as Int
            val rangeValues = createEmpty()
            for (value in minOfLeftOperand..maxOfRightOperand) {
                rangeValues.addAvailable(value)
            }
            return rangeValues
        }
        return createCantBeDefined()
    }

    private fun applyEachToEach(others: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues {
        if(this.areDefined && others.areDefined) {
            val results = availableValues.map { leftOp ->
                others.availableValues.map { rightOp -> operation(leftOp, rightOp) }
            }.flatten()
            return createFromCollection(results)
        }
        return createCantBeDefined()
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
        if(others.availableValues.size() > 1) {
            // this check means that in expression "x < y" only one element set is supported for "y"
            return undefinedWithFullRestrictions(valuesData)
        }
        val otherValue = others.availableValues.single()
        return if(availableValues.size() == 1) {
            val thisValue = availableValues.first()
            when {
                thisValue < otherValue -> BooleanVariableValue.True
                else -> BooleanVariableValue.False
            }
        } else {
            val thisArray = availableValues.toIntArray()
            thisArray.sort()
            when {
                thisArray.last() < otherValue -> BooleanVariableValue.True
                thisArray.last() == otherValue -> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val withoutLast = thisArray.copyOf(thisArray.size() - 1).toSet()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to withoutLast)
                        val onFalseRestrictions = mapOf(thisVarDescriptor to setOf(thisArray.last()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                }
                thisArray.last() > otherValue && otherValue > thisArray.first()-> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val bound = thisArray.indexOfFirst { it >= otherValue }
                        val lessValuesInThis = thisArray.copyOfRange(0, bound).toSet()
                        val greaterOrEqValuesInThis = thisArray.copyOfRange(bound, thisArray.size()).toSet()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to lessValuesInThis)
                        val onFalseRestrictions = mapOf(thisVarDescriptor to greaterOrEqValuesInThis)
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
                }
                thisArray.first() == otherValue -> {
                    if(thisVarDescriptor == null) {
                        undefinedWithFullRestrictions(valuesData)
                    } else {
                        val withoutFirst = thisArray.copyOfRange(1, thisArray.size()).toSet()
                        val onTrueRestrictions = mapOf(thisVarDescriptor to withoutFirst)
                        val onFalseRestrictions = mapOf(thisVarDescriptor to setOf(thisArray.first()))
                        BooleanVariableValue.Undefined(onTrueRestrictions, onFalseRestrictions)
                    }
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
        if(other !is IntegerVariableValues) {
            return false
        }
        return areEmpty.equals(other.areEmpty)
               && cantBeDefined.equals(other.cantBeDefined)
               && availableValues.equals(other.availableValues)
               && unavailableValues.equals(other.unavailableValues)
    }

    override fun hashCode(): Int {
        var code = 7
        code = 31 * code + areEmpty.hashCode()
        code = 31 * code + cantBeDefined.hashCode()
        code = 31 * code + availableValues.hashCode()
        code = 31 * code + unavailableValues.hashCode()
        return code
    }

    override fun toString(): String {
        return when {
            areEmpty -> "-"
            cantBeDefined -> "?"
            else -> "${availableValues.toString()}${unavailableValues.toString()}"
        }
    }

    companion object {
        public fun createEmpty(): IntegerVariableValues = IntegerVariableValues()
        public fun createSingleton(value: Int): IntegerVariableValues {
            val values = IntegerVariableValues()
            values.addAvailable(value)
            return values
        }
        public fun createCantBeDefined(): IntegerVariableValues {
            val values = createEmpty()
            values.setCantBeDefined()
            return values
        }
        public fun createFromCollection(collection: Collection<Int>): IntegerVariableValues {
            val values = createEmpty()
            collection.forEach { values.addAvailable(it) }
            return values
        }
    }

    //    private interface AvailabilityStatus
    //    private object ValueAvailable : AvailabilityStatus
    //    private class ValueUnavailable (val sinceLexicalScope: LexicalScope): AvailabilityStatus
    //
    //    private data class IntegerValue(val value: Int, var availabilityStatus: AvailabilityStatus) {
    //        override fun equals(other: Any?): Boolean {
    //            if(other !is IntegerValue) {
    //                return false
    //            }
    //            return value == other.value
    //        }
    //
    //        override fun hashCode(): Int = value
    //
    //        override fun toString(): String {
    //            val availability =
    //                    if(availabilityStatus is ValueUnavailable) {
    //                        val depth = (availabilityStatus as ValueUnavailable).sinceLexicalScope.depth
    //                        "($depth)"
    //                    } else ""
    //            return value.toString() + availability
    //        }
    //    }
    private data class UnavailableValue(val value: Int, val sinceLexicalScope: LexicalScope) {
        override fun toString(): String = "$value(${sinceLexicalScope.depth})"
    }
}
