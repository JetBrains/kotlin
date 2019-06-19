// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 until 8
    for (i in intProgression step 7) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L until 8L
    for (i in longProgression step 7L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' until 'h'
    for (i in charProgression step 7) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}