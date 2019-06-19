// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 5 downTo 1
    for (i in intProgression step 2 step 1) {
        intList += i
    }
    assertEquals(listOf(5, 4, 3, 2, 1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 5L downTo 1L
    for (i in longProgression step 2L step 1L) {
        longList += i
    }
    assertEquals(listOf(5L, 4L, 3L, 2L, 1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'e' downTo 'a'
    for (i in charProgression step 2 step 1) {
        charList += i
    }
    assertEquals(listOf('e', 'd', 'c', 'b', 'a'), charList)

    return "OK"
}