// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 until 5
    for (i in intProgression step 1) {
        intList += i
    }
    assertEquals(listOf(1, 2, 3, 4), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L until 5L
    for (i in longProgression step 1L) {
        longList += i
    }
    assertEquals(listOf(1L, 2L, 3L, 4L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' until 'e'
    for (i in charProgression step 1) {
        charList += i
    }
    assertEquals(listOf('a', 'b', 'c', 'd'), charList)

    return "OK"
}