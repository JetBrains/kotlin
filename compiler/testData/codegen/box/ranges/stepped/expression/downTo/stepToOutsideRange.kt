// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 7 downTo 1
    for (i in intProgression step 7) {
        intList += i
    }
    assertEquals(listOf(7), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 7L downTo 1L
    for (i in longProgression step 7L) {
        longList += i
    }
    assertEquals(listOf(7L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'g' downTo 'a'
    for (i in charProgression step 7) {
        charList += i
    }
    assertEquals(listOf('g'), charList)

    return "OK"
}