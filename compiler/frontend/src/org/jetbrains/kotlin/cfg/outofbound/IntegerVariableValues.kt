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

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import java.util.*

public interface IntegerVariableValues {
    public fun copy(): IntegerVariableValues = this
    public fun merge(values: IntegerVariableValues): IntegerVariableValues
    // operators
    public fun plus(others: IntegerVariableValues): IntegerVariableValues = Undefined
    public fun minus(others: IntegerVariableValues): IntegerVariableValues = Undefined
    public fun times(others: IntegerVariableValues): IntegerVariableValues = Undefined
    public fun div(others: IntegerVariableValues): IntegerVariableValues = Undefined
    public fun rangeTo(others: IntegerVariableValues): IntegerVariableValues = Undefined
    public fun minus(): IntegerVariableValues = Undefined

    // special operators (IntegerValues, IntegerValues) -> BooleanVariableValue
    public fun eq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions
    public fun notEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions
    public fun lessThan(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions
    public fun greaterThan(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions
    public fun greaterOrEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions
    public fun lessOrEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData): BooleanVariableValue =
            BooleanVariableValue.undefinedWithNoRestrictions

    // Represents a value of Integer variable that is not initialized
    public object Uninitialized : IntegerVariableValues {
        override fun toString(): String = "-"

        override fun merge(values: IntegerVariableValues): IntegerVariableValues =
                when (values) {
                    is Defined -> values.merge(this)
                    is Dead -> Uninitialized
                    else -> values
                }
    }

    // Represents a value of Integer variable that is obtained from constructions analysis don't process
    // (for ex, in `a = foo()` `a` has an Undefined value, because function calls are not processed)
    public object Undefined : IntegerVariableValues {
        override fun toString(): String = "?"

        override fun merge(values: IntegerVariableValues): IntegerVariableValues =
                when (values) {
                    is Defined -> values.merge(this)
                    else -> Undefined
                }
    }

    // Represent a value of Integer variable inside the block of dead code
    public object Dead : IntegerVariableValues {
        override fun plus(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun minus(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun times(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun div(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun rangeTo(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun minus(): IntegerVariableValues = Dead

        override fun toString(): String = "#"

        override fun merge(values: IntegerVariableValues): IntegerVariableValues =
                when (values) {
                    is Defined -> values.merge(this)
                    else -> values
                }
    }

    // Represent a set of values Integer variable can have
    public class Defined private constructor() : IntegerVariableValues {
        public constructor(value: Int) : this() {
            // the `values` set is always non empty
            this.add(value)
        }

        private constructor(collection: Collection<Int>) : this() {
            assert(collection.size() != 0, "IntegerValues.Defined can't be created with no value")
            collection.forEach { this.add(it) }
        }

        private val values: MutableSet<Int> = HashSet()
        private val possibleValuesThreshold = 2
        // `values` set may contain no more than `possibleValuesThreshold` values.
        // If there was attempt to add more values they would not be added and the flag below would be set
        public var allPossibleValuesKnown: Boolean = true
            private set

        override fun copy(): IntegerVariableValues.Defined {
            val copy = Defined()
            copy.values.addAll(this.values)
            copy.allPossibleValuesKnown = this.allPossibleValuesKnown
            return copy
        }

        override fun merge(values: IntegerVariableValues): IntegerVariableValues {
            when (values) {
                is Uninitialized,
                is Undefined -> this.allPossibleValuesKnown = false
                is Defined -> {
                    values.values.forEach { this.add(it) }
                    if (!values.allPossibleValuesKnown) {
                        this.allPossibleValuesKnown = false
                    }
                }
                else -> {
                    assert(values is Dead, "IntegerValues has unexpected derived class")
                }
            }
            return this
        }

        public fun getValues(): List<Int> = values.toList()

        public fun leaveOnlyValuesInSet(valuesToLeave: Set<Int>): IntegerVariableValues {
            val currentAvailableValues = LinkedList<Int>()
            currentAvailableValues.addAll(values)
            currentAvailableValues.forEach {
                if (!valuesToLeave.contains(it)) {
                    values.remove(it)
                }
            }
            if (values.isEmpty()) {
                return Undefined
            }
            return this
        }

        private fun add(value: Int) {
            if (!values.contains(value)) {
                if (values.size() == possibleValuesThreshold) {
                    allPossibleValuesKnown = false
                    val maxValue = values.max() as Int
                    if (value > maxValue) {
                        values.remove(maxValue)
                        values.add(value)
                    }
                    val minValue = values.min() as Int
                    if (value < minValue) {
                        values.remove(minValue)
                        values.add(value)
                    }
                }
                else {
                    values.add(value)
                }
            }
        }

        // operators overloading
        override fun plus(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x + y }
        override fun minus(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x - y }
        override fun times(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x * y }
        override fun div(others: IntegerVariableValues): IntegerVariableValues =
                if (others is Defined) {
                    val nonZero = others.values.filter { it != 0 }
                    if (nonZero.isEmpty()) Undefined
                    else applyEachToEach(Defined(nonZero), { x, y -> x / y })
                }
                else Undefined
        override fun rangeTo(others: IntegerVariableValues): IntegerVariableValues {
            if (others is Defined) {
                val minOfLeftOperand = values.min() as Int
                val maxOfRightOperand = others.values.max() as Int
                val rangeValues = LinkedList<Int>()
                for (value in minOfLeftOperand..maxOfRightOperand) {
                    rangeValues.add(value)
                }
                return if (rangeValues.isEmpty()) Undefined else Defined(rangeValues)
            }
            return Undefined
        }

        override fun minus(): IntegerVariableValues = Defined(this.values.map { -1 * it })

        private fun applyEachToEach(others: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues =
                if (others is Defined) {
                    val results = values.map { leftOp ->
                        others.values.map { rightOp -> operation(leftOp, rightOp) }
                    }.flatten()
                    Defined(results)
                }
                else Undefined

        override fun eq(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyComparisonIfArgsAreAppropriate(other, valuesData) { valueToCompareWith ->
                    thisVarDescriptor?.let {
                        val thisValues = HashSet(values)
                        val onTrueValues = if (thisValues.contains(valueToCompareWith)) setOf(valueToCompareWith) else setOf()
                        thisValues.remove(valueToCompareWith)
                        if (this.allPossibleValuesKnown) {
                            if (onTrueValues.isEmpty()) {
                                return@applyComparisonIfArgsAreAppropriate BooleanVariableValue.False
                            }
                            else if (thisValues.isEmpty()) {
                                return@applyComparisonIfArgsAreAppropriate BooleanVariableValue.True
                            }
                        }
                        BooleanVariableValue.Undefined(mapOf(it to onTrueValues), mapOf(it to thisValues))
                    } ?: undefinedWithFullRestrictions(valuesData)
                }

        override fun notEq(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyNot(eq(other, thisVarDescriptor, valuesData))

        override fun lessThan(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyComparisonIfArgsAreAppropriate(other, valuesData) { valueToCompareWith ->
                    comparison(valueToCompareWith, thisVarDescriptor, valuesData,
                               { array, value -> array.indexOfFirst { it >= value } },
                               { varDescriptor, valuesWithLessIndices, valuesWithGreaterOrEqIndices ->
                                   if (this.allPossibleValuesKnown) {
                                       if (valuesWithLessIndices.isEmpty()) {
                                           return@comparison BooleanVariableValue.False
                                       }
                                       else if (valuesWithGreaterOrEqIndices.isEmpty()) {
                                           return@comparison BooleanVariableValue.True
                                       }
                                   }
                                   BooleanVariableValue.Undefined (
                                           mapOf(varDescriptor to valuesWithLessIndices),
                                           mapOf(varDescriptor to valuesWithGreaterOrEqIndices)
                                   )

                               }
                    )
                }

        override fun greaterThan(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyComparisonIfArgsAreAppropriate(other, valuesData) { valueToCompareWith ->
                    comparison(valueToCompareWith, thisVarDescriptor, valuesData,
                               { array, value -> array.indexOfFirst { it > value } },
                               { varDescriptor, valuesWithLessIndices, valuesWithGreaterOrEqIndices ->
                                   if (this.allPossibleValuesKnown) {
                                       if (valuesWithLessIndices.isEmpty()) {
                                           return@comparison BooleanVariableValue.True
                                       }
                                       else if (valuesWithGreaterOrEqIndices.isEmpty()) {
                                           return@comparison BooleanVariableValue.False
                                       }
                                   }
                                   BooleanVariableValue.Undefined(
                                           mapOf(varDescriptor to valuesWithGreaterOrEqIndices),
                                           mapOf(varDescriptor to valuesWithLessIndices)
                                   )
                               }
                    )
                }

        override fun greaterOrEq(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyNot(lessThan(other, thisVarDescriptor, valuesData))

        override fun lessOrEq(
                other: IntegerVariableValues,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData
        ): BooleanVariableValue =
                applyNot(greaterThan(other, thisVarDescriptor, valuesData))

        private fun comparison(
                otherValue: Int,
                thisVarDescriptor: VariableDescriptor?,
                valuesData: ValuesData,
                findIndex: (IntArray, Int) -> Int,
                createBoolean: (VariableDescriptor, Set<Int>, Set<Int>) -> BooleanVariableValue
        ): BooleanVariableValue {
            val thisArray = this.values.toIntArray()
            thisArray.sort()
            return thisVarDescriptor?.let {
                val foundIndex = findIndex(thisArray, otherValue)
                val bound = if (foundIndex < 0) thisArray.size() else foundIndex
                val valuesWithLessIndices = thisArray.copyOfRange(0, bound).toSet()
                val valuesWithGreaterOrEqIndices = thisArray.copyOfRange(bound, thisArray.size()).toSet()
                createBoolean(it, valuesWithLessIndices, valuesWithGreaterOrEqIndices)
            } ?: undefinedWithFullRestrictions(valuesData)
        }

        private fun applyComparisonIfArgsAreAppropriate(
                other: IntegerVariableValues,
                valuesData: ValuesData,
                comparison: (Int) -> BooleanVariableValue
        ): BooleanVariableValue {
            if (other !is Defined || other.values.size() > 1) {
                // the second check means that in expression "x 'operator' y" only one element set is supported for "y"
                return undefinedWithFullRestrictions(valuesData)
            }
            return comparison(other.values.single())
        }

        private fun applyNot(booleanValue: BooleanVariableValue): BooleanVariableValue =
            if (booleanValue is BooleanVariableValue.Undefined) {
                BooleanVariableValue.Undefined(booleanValue.onFalseRestrictions, booleanValue.onTrueRestrictions)
            }
            else if (booleanValue is BooleanVariableValue.False) {
                BooleanVariableValue.True
            }
            else {
                assert(booleanValue is BooleanVariableValue.True, "Unexpected derived type of BooleanVariableValue")
                BooleanVariableValue.False
            }

        private fun undefinedWithFullRestrictions(valuesData: ValuesData): BooleanVariableValue.Undefined {
            val restrictions = valuesData.intVarsToValues.keySet()
                    .map { Pair(it, setOf<Int>()) }
                    .toMap()
            return BooleanVariableValue.Undefined(restrictions, restrictions)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Defined) {
                return false
            }
            return this.allPossibleValuesKnown == other.allPossibleValuesKnown &&
                   this.values == other.values
        }

        override fun hashCode(): Int {
            var code = 7
            code = 31 * code + this.allPossibleValuesKnown.hashCode()
            code = 31 * code + this.values.hashCode()
            return code
        }

        override fun toString(): String {
            val listAsString = "${this.values.toSortedList().toString()}"
            if (this.allPossibleValuesKnown) {
                return listAsString
            }
            return "${listAsString.dropLast(1)}, ?]"
        }
    }
}
