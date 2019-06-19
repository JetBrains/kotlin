// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1..Int.MAX_VALUE step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L..Long.MAX_VALUE step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 1.toChar()..Char.MAX_VALUE step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(1.toChar()), charList)

    return "OK"
}