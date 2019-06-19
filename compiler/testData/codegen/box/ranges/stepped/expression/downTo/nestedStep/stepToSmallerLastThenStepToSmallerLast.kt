// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 10 downTo 1
    for (i in intProgression step 2 step 3) {
        intList += i
    }
    assertEquals(listOf(10, 7, 4), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 10L downTo 1L
    for (i in longProgression step 2L step 3L) {
        longList += i
    }
    assertEquals(listOf(10L, 7L, 4L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'j' downTo 'a'
    for (i in charProgression step 2 step 3) {
        charList += i
    }
    assertEquals(listOf('j', 'g', 'd'), charList)

    return "OK"
}