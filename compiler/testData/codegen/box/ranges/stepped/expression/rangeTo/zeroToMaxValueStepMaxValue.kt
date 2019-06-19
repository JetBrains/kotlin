// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 0..Int.MAX_VALUE
    for (i in intProgression step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(0, Int.MAX_VALUE), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 0L..Long.MAX_VALUE
    for (i in longProgression step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(0L, Long.MAX_VALUE), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 0.toChar()..Char.MAX_VALUE
    for (i in charProgression step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(0.toChar(), Char.MAX_VALUE), charList)

    return "OK"
}