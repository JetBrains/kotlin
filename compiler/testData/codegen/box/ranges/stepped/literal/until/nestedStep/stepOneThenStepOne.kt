// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 until 5 step 1 step 1) {
        intList += i
    }
    assertEquals(listOf(1, 2, 3, 4), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L until 5L step 1L step 1L) {
        longList += i
    }
    assertEquals(listOf(1L, 2L, 3L, 4L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' until 'e' step 1 step 1) {
        charList += i
    }
    assertEquals(listOf('a', 'b', 'c', 'd'), charList)

    return "OK"
}