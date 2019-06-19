// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 until 9
    for (i in (intProgression step 2).reversed()) {
        intList += i
    }
    assertEquals(listOf(7, 5, 3, 1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L until 9L
    for (i in (longProgression step 2L).reversed()) {
        longList += i
    }
    assertEquals(listOf(7L, 5L, 3L, 1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' until 'i'
    for (i in (charProgression step 2).reversed()) {
        charList += i
    }
    assertEquals(listOf('g', 'e', 'c', 'a'), charList)

    return "OK"
}