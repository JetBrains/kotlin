// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1..8 step 2 step 3) {
        intList += i
    }
    assertEquals(listOf(1, 4, 7), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L..8L step 2L step 3L) {
        longList += i
    }
    assertEquals(listOf(1L, 4L, 7L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a'..'h' step 2 step 3) {
        charList += i
    }
    assertEquals(listOf('a', 'd', 'g'), charList)

    return "OK"
}