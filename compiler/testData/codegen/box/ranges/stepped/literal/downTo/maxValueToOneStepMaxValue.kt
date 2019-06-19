// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in Int.MAX_VALUE downTo 1 step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(Int.MAX_VALUE), intList)

    val longList = mutableListOf<Long>()
    for (i in Long.MAX_VALUE downTo 1L step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(Long.MAX_VALUE), longList)

    val charList = mutableListOf<Char>()
    for (i in Char.MAX_VALUE downTo 1.toChar() step Char.MAX_VALUE.toInt()) {
        charList += i
    }
    assertEquals(listOf(Char.MAX_VALUE), charList)

    return "OK"
}