// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1..5 step 2 step 1) {
        intList += i
    }
    assertEquals(listOf(1, 2, 3, 4, 5), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L..5L step 2L step 1L) {
        longList += i
    }
    assertEquals(listOf(1L, 2L, 3L, 4L, 5L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a'..'e' step 2 step 1) {
        charList += i
    }
    assertEquals(listOf('a', 'b', 'c', 'd', 'e'), charList)

    return "OK"
}