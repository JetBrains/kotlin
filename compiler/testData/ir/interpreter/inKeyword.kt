import kotlin.collections.*

@CompileTimeCalculation
fun numberIsInArray(array: IntArray, number: Int): Boolean {
    return number in array
}

@CompileTimeCalculation
fun valueIsInArray(array: Array<Any>, value: Any?): Boolean {
    return value in array
}

const val a1 = <!EVALUATED: `true`!>numberIsInArray(intArrayOf(1, 2, 3), 1)<!>
const val a2 = <!EVALUATED: `false`!>numberIsInArray(intArrayOf(1, 2, 3), -1)<!>

const val b1 = <!EVALUATED: `true`!>valueIsInArray(arrayOf(1, 2, 3), 1)<!>
const val b2 = <!EVALUATED: `false`!>valueIsInArray(arrayOf(1, 2, 3), -1)<!>
const val b3 = <!EVALUATED: `true`!>valueIsInArray(arrayOf(1, 2.0f, "3"), "3")<!>
const val b4 = <!EVALUATED: `false`!>valueIsInArray(arrayOf(1, 2.0f, "3"), null)<!>
const val b5 = <!EVALUATED: `false`!>valueIsInArray(arrayOf(1, 2.0f, "3"), 1.0f)<!>
