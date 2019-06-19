// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 8 downTo 1
    for (i in intProgression step 2 step 3) {
        intList += i
    }
    assertEquals(listOf(8, 5, 2), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 8L downTo 1L
    for (i in longProgression step 2L step 3L) {
        longList += i
    }
    assertEquals(listOf(8L, 5L, 2L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'h' downTo 'a'
    for (i in charProgression step 2 step 3) {
        charList += i
    }
    assertEquals(listOf('h', 'e', 'b'), charList)

    return "OK"
}