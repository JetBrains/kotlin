// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 until 11 step 3 step 2) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7, 9), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L until 11L step 3L step 2L) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L, 9L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' until 'k' step 3 step 2) {
        charList += i
    }
    assertEquals(listOf('a', 'c', 'e', 'g', 'i'), charList)

    return "OK"
}