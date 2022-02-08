import kotlin.collections.*

const val a1 = setOf(1, 2, 3).<!EVALUATED: `3`!>size<!>
const val a2 = setOf(1, 2, 3, 3, 2, 1).<!EVALUATED: `3`!>size<!>
const val b = emptySet<Int>().<!EVALUATED: `0`!>size<!>
const val c = setOf<Int>().<!EVALUATED: `0`!>hashCode()<!>

@CompileTimeCalculation
fun getSum(set: Set<Int>): Int {
    var sum: Int = 0
    for (element in set) {
        sum += element
    }
    return sum
}

const val sum = <!EVALUATED: `16`!>getSum(setOf(1, 3, 5, 7, 7, 5))<!>
