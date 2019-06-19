// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun two() = 2

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 until 9 step two()) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L until 9L step two().toLong()) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' until 'i' step two()) {
        charList += i
    }
    assertEquals(listOf('a', 'c', 'e', 'g'), charList)

    return "OK"
}