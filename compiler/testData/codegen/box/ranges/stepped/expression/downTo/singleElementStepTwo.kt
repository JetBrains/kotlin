// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 downTo 1
    for (i in intProgression step 2) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L downTo 1L
    for (i in longProgression step 2L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' downTo 'a'
    for (i in charProgression step 2) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}