// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in (1..8 step 2).reversed()) {
        intList += i
    }
    assertEquals(listOf(7, 5, 3, 1), intList)

    val longList = mutableListOf<Long>()
    for (i in (1L..8L step 2L).reversed()) {
        longList += i
    }
    assertEquals(listOf(7L, 5L, 3L, 1L), longList)

    val charList = mutableListOf<Char>()
    for (i in ('a'..'h' step 2).reversed()) {
        charList += i
    }
    assertEquals(listOf('g', 'e', 'c', 'a'), charList)

    return "OK"
}