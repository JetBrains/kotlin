// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 until 11
    for (i in intProgression step 3 step 2) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7, 9), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L until 11L
    for (i in longProgression step 3L step 2L) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L, 9L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' until 'k'
    for (i in charProgression step 3 step 2) {
        charList += i
    }
    assertEquals(listOf('a', 'c', 'e', 'g', 'i'), charList)

    return "OK"
}