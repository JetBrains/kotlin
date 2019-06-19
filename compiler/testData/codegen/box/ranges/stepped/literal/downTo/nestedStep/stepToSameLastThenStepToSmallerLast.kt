// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 10 downTo 1 step 3 step 2) {
        intList += i
    }
    assertEquals(listOf(10, 8, 6, 4, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in 10L downTo 1L step 3L step 2L) {
        longList += i
    }
    assertEquals(listOf(10L, 8L, 6L, 4L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'j' downTo 'a' step 3 step 2) {
        charList += i
    }
    assertEquals(listOf('j', 'h', 'f', 'd', 'b'), charList)

    return "OK"
}