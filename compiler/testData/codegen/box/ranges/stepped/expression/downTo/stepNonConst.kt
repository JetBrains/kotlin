// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun two() = 2

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 8 downTo 1
    for (i in intProgression step two()) {
        intList += i
    }
    assertEquals(listOf(8, 6, 4, 2), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 8L downTo 1L
    for (i in longProgression step two().toLong()) {
        longList += i
    }
    assertEquals(listOf(8L, 6L, 4L, 2L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'h' downTo 'a'
    for (i in charProgression step two()) {
        charList += i
    }
    assertEquals(listOf('h', 'f', 'd', 'b'), charList)

    return "OK"
}