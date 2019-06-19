// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 5 downTo 1 step 2 step 1) {
        intList += i
    }
    assertEquals(listOf(5, 4, 3, 2, 1), intList)

    val longList = mutableListOf<Long>()
    for (i in 5L downTo 1L step 2L step 1L) {
        longList += i
    }
    assertEquals(listOf(5L, 4L, 3L, 2L, 1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'e' downTo 'a' step 2 step 1) {
        charList += i
    }
    assertEquals(listOf('e', 'd', 'c', 'b', 'a'), charList)

    return "OK"
}