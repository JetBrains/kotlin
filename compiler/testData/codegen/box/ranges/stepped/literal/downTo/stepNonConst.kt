// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun two() = 2

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 8 downTo 1 step two()) {
        intList += i
    }
    assertEquals(listOf(8, 6, 4, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in 8L downTo 1L step two().toLong()) {
        longList += i
    }
    assertEquals(listOf(8L, 6L, 4L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'h' downTo 'a' step two()) {
        charList += i
    }
    assertEquals(listOf('h', 'f', 'd', 'b'), charList)

    return "OK"
}