// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = Int.MAX_VALUE downTo 1
    for (i in intProgression step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(Int.MAX_VALUE), intList)

    val longList = mutableListOf<Long>()
    val longProgression = Long.MAX_VALUE downTo 1L
    for (i in longProgression step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(Long.MAX_VALUE), longList)

    val charList = mutableListOf<Char>()
    val charProgression = Char.MAX_VALUE downTo 1.toChar()
    for (i in charProgression step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(Char.MAX_VALUE), charList)

    return "OK"
}