import kotlin.collections.*

const val a = listOf(1, 2, 3).<!EVALUATED: `3`!>size<!>
const val b = emptyList<Int>().<!EVALUATED: `0`!>size<!>
const val c = listOf<Int>().<!EVALUATED: `1`!>hashCode()<!>

@CompileTimeCalculation
fun getSum(list: List<Int>): Int {
    var sum: Int = 0
    for (element in list) {
        sum += element
    }
    return sum
}

const val sum = <!EVALUATED: `16`!>getSum(listOf(1, 3, 5, 7))<!>
