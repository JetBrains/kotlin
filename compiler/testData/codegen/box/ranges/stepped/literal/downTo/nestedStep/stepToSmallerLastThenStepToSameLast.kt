// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 8 downTo 1 step 2 step 3) {
        intList += i
    }
    assertEquals(listOf(8, 5, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in 8L downTo 1L step 2L step 3L) {
        longList += i
    }
    assertEquals(listOf(8L, 5L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'h' downTo 'a' step 2 step 3) {
        charList += i
    }
    assertEquals(listOf('h', 'e', 'b'), charList)

    return "OK"
}