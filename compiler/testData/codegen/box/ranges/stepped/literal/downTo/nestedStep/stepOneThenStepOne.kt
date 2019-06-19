// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 4 downTo 1 step 1 step 1) {
        intList += i
    }
    assertEquals(listOf(4, 3, 2, 1), intList)

    val longList = mutableListOf<Long>()
    for (i in 4L downTo 1L step 1L step 1L) {
        longList += i
    }
    assertEquals(listOf(4L, 3L, 2L, 1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'd' downTo 'a' step 1 step 1) {
        charList += i
    }
    assertEquals(listOf('d', 'c', 'b', 'a'), charList)

    return "OK"
}