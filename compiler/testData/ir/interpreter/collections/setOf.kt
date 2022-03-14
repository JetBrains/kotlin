import kotlin.collections.*

const val a1 = <!EVALUATED: `3`!>setOf(1, 2, 3).size<!>
const val a2 = <!EVALUATED: `3`!>setOf(1, 2, 3, 3, 2, 1).size<!>
const val b = <!EVALUATED: `0`!>emptySet<Int>().size<!>
const val c = <!EVALUATED: `0`!>setOf<Int>().hashCode()<!>

@CompileTimeCalculation
fun getSum(set: Set<Int>): Int {
    var sum: Int = 0
    for (element in set) {
        sum += element
    }
    return sum
}

const val sum = <!EVALUATED: `16`!>getSum(setOf(1, 3, 5, 7, 7, 5))<!>
