// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 6 downTo 1 step 2 step 1) {
        intList += i
    }
    assertEquals(listOf(6, 5, 4, 3, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in 6L downTo 1L step 2L step 1L) {
        longList += i
    }
    assertEquals(listOf(6L, 5L, 4L, 3L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'f' downTo 'a' step 2 step 1) {
        charList += i
    }
    assertEquals(listOf('f', 'e', 'd', 'c', 'b'), charList)

    return "OK"
}