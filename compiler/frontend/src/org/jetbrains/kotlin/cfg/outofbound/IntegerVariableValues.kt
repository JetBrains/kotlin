package org.jetbrains.kotlin.cfg.outofbound

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import java.util.HashSet

public sealed class IntegerVariableValues private constructor() {
    public abstract fun merge(other: IntegerVariableValues): IntegerVariableValues
    public fun copy(): IntegerVariableValues = this // if some subclass is mutable, it should override this method

    // operators
    public open fun plus(others: IntegerVariableValues): IntegerVariableValues = IntegerVariableValues.Undefined
    public open fun minus(others: IntegerVariableValues): IntegerVariableValues = IntegerVariableValues.Undefined
    public open fun times(others: IntegerVariableValues): IntegerVariableValues = IntegerVariableValues.Undefined
    public open fun div(others: IntegerVariableValues): IntegerVariableValues = IntegerVariableValues.Undefined
    public open fun rangeTo(others: IntegerVariableValues): IntegerVariableValues = IntegerVariableValues.Undefined
    public open fun minus(): IntegerVariableValues = IntegerVariableValues.Undefined

    // special operators (IntegerValues, IntegerValues) -> BooleanVariableValue
    public open fun eq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions
    public open fun notEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions
    public open fun lessThan(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions
    public open fun greaterThan(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions
    public open fun greaterOrEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions
    public open fun lessOrEq(other: IntegerVariableValues, thisVarDescriptor: VariableDescriptor?, valuesData: ValuesData)
            : BooleanVariableValue = BooleanVariableValue.undefinedWithNoRestrictions

    // Represents a value of Integer variable that is not initialized
    public object Uninitialized : IntegerVariableValues() {
        override fun toString(): String = "-"

        override fun merge(other: IntegerVariableValues): IntegerVariableValues =
                when (other) {
                    is IntegerVariableValues.Defined -> other.merge(this)
                    is Dead -> Uninitialized
                    else -> other
                }
    }

    // Represents a value of Integer variable that is obtained from constructions analysis don't process
    // (for ex, in `a = foo()` `a` has an Undefined value, because function calls are not processed)
    public object Undefined : IntegerVariableValues() {
        override fun toString(): String = "?"

        override fun merge(other: IntegerVariableValues): IntegerVariableValues =
                when (other) {
                    is IntegerVariableValues.Defined -> other.merge(this)
                    else -> Undefined
                }
    }

    // Represent a value of Integer variable inside the block of dead code
    public object Dead : IntegerVariableValues() {
        override fun toString(): String = "#"

        override fun merge(other: IntegerVariableValues): IntegerVariableValues =
                when (other) {
                    is IntegerVariableValues.Defined -> other.merge(this)
                    else -> other
                }

        override fun plus(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun minus(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun times(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun div(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun rangeTo(others: IntegerVariableValues): IntegerVariableValues = Dead
        override fun minus(): IntegerVariableValues = Dead
    }

    // Represent a set of values Integer variable can have
    public class Defined private constructor(
            possibleValues: Set<Int>,
            forceNotAllValuesKnown: Boolean = false
    ) : IntegerVariableValues() {
        companion object {
            private val POSSIBLE_VALUES_THRESHOLD = 2
        }

        public val values: Set<Int>
        // `values` set may contain no more than `POSSIBLE_VALUES_THRESHOLD` values.
        // If there was attempt to add more values they would not be added and the flag below would be set
        public val allPossibleValuesKnown: Boolean

        init {
            assert(POSSIBLE_VALUES_THRESHOLD > 0, "Possible values threshold must be positive number")
            assert(!possibleValues.isEmpty(), "IntegerVariableValues.Defined must contain at least one value")
            this.allPossibleValuesKnown = if (forceNotAllValuesKnown) false else possibleValues.size() <= POSSIBLE_VALUES_THRESHOLD
            this.values =
                if (this.allPossibleValuesKnown)
                    possibleValues
                else {
                    val original = possibleValues.toSortedList()
                    val elementsToTake = Math.ceil(POSSIBLE_VALUES_THRESHOLD / 2.0).toInt()
                    val minValues = original.take(elementsToTake)
                    val maxValues = original.reverse().take(elementsToTake)
                    if (POSSIBLE_VALUES_THRESHOLD % 2 == 1)
                        (maxValues + minValues.dropLast(1)).toSet()
                    else
                        (maxValues + minValues).toSet()
                }
        }

        public constructor(possibleValue: Int) : this(setOf(possibleValue))

        override fun equals(other: Any?): Boolean {
            return this identityEquals other ||
                   other is Defined &&
                   this.allPossibleValuesKnown == other.allPossibleValuesKnown &&
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

        override fun merge(other: IntegerVariableValues): IntegerVariableValues =
            when (other) {
                is IntegerVariableValues.Uninitialized,
                is IntegerVariableValues.Undefined -> this.toValueWithNotAllPossibleValuesKnown()
                is Defined -> {
                    val notAllValuesKnown = !this.allPossibleValuesKnown || !other.allPossibleValuesKnown
                    Defined(this.values + other.values, forceNotAllValuesKnown = notAllValuesKnown)
                }
                is Dead -> this.copy()
            }

        public fun leaveOnlyValuesInSet(valuesToLeave: Set<Int>): IntegerVariableValues {
            val intersection = this.values.intersect(valuesToLeave)
            return if (intersection.isEmpty()) Undefined
            else Defined(intersection, forceNotAllValuesKnown = !this.allPossibleValuesKnown)
        }

        public fun toValueWithNotAllPossibleValuesKnown(): Defined = Defined(this.values, forceNotAllValuesKnown = true)

        // operators overloading
        override fun plus(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x + y }
        override fun minus(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x - y }
        override fun times(others: IntegerVariableValues): IntegerVariableValues = applyEachToEach(others) { x, y -> x * y }

        override fun div(others: IntegerVariableValues): IntegerVariableValues =
                if (others !is Defined)
                    IntegerVariableValues.Undefined
                else {
                    val nonZero = others.values.filter { it != 0 }.toSet()
                    if (nonZero.isEmpty())
                        IntegerVariableValues.Undefined
                    else {
                        val noneZeroOthers = Defined(nonZero, forceNotAllValuesKnown = !others.allPossibleValuesKnown)
                        applyEachToEach(noneZeroOthers) { x, y -> x / y }
                    }
                }

        override fun rangeTo(others: IntegerVariableValues): IntegerVariableValues =
            if (others !is Defined)
                IntegerVariableValues.Undefined
            else {
                val minOfLeftOperand = this.values.min() as Int
                val maxOfRightOperand = others.values.max() as Int
                val rangeValues = hashSetOf<Int>()
                for (value in minOfLeftOperand..maxOfRightOperand) { rangeValues.add(value) }
                if (rangeValues.isEmpty())
                    IntegerVariableValues.Undefined
                else
                    Defined(rangeValues, forceNotAllValuesKnown = !this.allPossibleValuesKnown || !others.allPossibleValuesKnown)
            }

        override fun minus(): IntegerVariableValues {
            val valuesSet = this.values.map { -1 * it }.toSet()
            return Defined(valuesSet, forceNotAllValuesKnown = !this.allPossibleValuesKnown)
        }

        private fun applyEachToEach(other: IntegerVariableValues, operation: (Int, Int) -> Int): IntegerVariableValues =
                if (other is Defined) {
                    val results = this.values
                            .map { leftOp -> other.values.map { rightOp -> operation(leftOp, rightOp) } }
                            .flatten()
                            .toSet()
                    Defined(results, forceNotAllValuesKnown = !this.allPossibleValuesKnown || !other.allPossibleValuesKnown)
                }
                else IntegerVariableValues.Undefined

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
                if (booleanValue is BooleanVariableValue.Undefined)
                    BooleanVariableValue.Undefined(booleanValue.onFalseRestrictions, booleanValue.onTrueRestrictions)
                else if (booleanValue is BooleanVariableValue.False)
                    BooleanVariableValue.True
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
    }
}