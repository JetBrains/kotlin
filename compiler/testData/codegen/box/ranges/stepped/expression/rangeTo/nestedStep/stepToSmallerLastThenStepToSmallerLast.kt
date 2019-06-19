// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..10
    for (i in intProgression step 2 step 3) {
        intList += i
    }
    assertEquals(listOf(1, 4, 7), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..10L
    for (i in longProgression step 2L step 3L) {
        longList += i
    }
    assertEquals(listOf(1L, 4L, 7L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..'j'
    for (i in charProgression step 2 step 3) {
        charList += i
    }
    assertEquals(listOf('a', 'd', 'g'), charList)

    return "OK"
}