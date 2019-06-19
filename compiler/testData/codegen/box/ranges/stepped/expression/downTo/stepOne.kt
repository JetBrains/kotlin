// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 4 downTo 1
    for (i in intProgression step 1) {
        intList += i
    }
    assertEquals(listOf(4, 3, 2, 1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 4L downTo 1L
    for (i in longProgression step 1L) {
        longList += i
    }
    assertEquals(listOf(4L, 3L, 2L, 1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'd' downTo 'a'
    for (i in charProgression step 1) {
        charList += i
    }
    assertEquals(listOf('d', 'c', 'b', 'a'), charList)

    return "OK"
}