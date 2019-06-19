// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in Int.MIN_VALUE..Int.MAX_VALUE step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(Int.MIN_VALUE, -1, Int.MAX_VALUE - 1), intList)

    val longList = mutableListOf<Long>()
    for (i in Long.MIN_VALUE..Long.MAX_VALUE step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1), longList)

    val charList = mutableListOf<Char>()
    for (i in Char.MIN_VALUE..Char.MAX_VALUE step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(Char.MIN_VALUE, Char.MAX_VALUE), charList)

    return "OK"
}