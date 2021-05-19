import kotlin.*

@CompileTimeCalculation
fun getArrayElement(array: IntArray, index: Int): Int {
    try {
        return array[index]
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException("Index must be less than array size and greater than zero", e)
    }
}

const val a = <!EVALUATED: `-1`!>try {
    getArrayElement(intArrayOf(1, 2, 3), -1).let { "Element at given index is " + it }
} catch (e: Exception) {
    e.cause?.message ?: "Exception without message"
}<!>

const val b = <!EVALUATED: `Element at given index is 1`!>try {
    getArrayElement(intArrayOf(1, 2, 3), 0).let { "Element at given index is " + it }
} catch (e: Exception) {
    e.cause?.message ?: "Exception without message"
}<!>
