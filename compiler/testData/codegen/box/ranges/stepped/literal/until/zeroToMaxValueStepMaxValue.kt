// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 0 until Int.MAX_VALUE step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(0), intList)

    val longList = mutableListOf<Long>()
    for (i in 0L until Long.MAX_VALUE step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(0L), longList)

    val charList = mutableListOf<Char>()
    for (i in 0.toChar() until Char.MAX_VALUE step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(0.toChar()), charList)

    return "OK"
}