// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun two() = 2

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..8
    for (i in intProgression step two()) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..8L
    for (i in longProgression step two().toLong()) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..'h'
    for (i in charProgression step two()) {
        charList += i
    }
    assertEquals(listOf('a', 'c', 'e', 'g'), charList)

    return "OK"
}