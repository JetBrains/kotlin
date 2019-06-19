// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in Int.MAX_VALUE downTo Int.MIN_VALUE step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(Int.MAX_VALUE, 0, Int.MIN_VALUE + 1), intList)

    val longList = mutableListOf<Long>()
    for (i in Long.MAX_VALUE downTo Long.MIN_VALUE step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(Long.MAX_VALUE, 0, Long.MIN_VALUE + 1), longList)

    val charList = mutableListOf<Char>()
    for (i in Char.MAX_VALUE downTo Char.MIN_VALUE step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(Char.MAX_VALUE, Char.MIN_VALUE), charList)

    return "OK"
}