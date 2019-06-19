// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 2 until Int.MIN_VALUE step 2) {
        intList += i
    }
    assertTrue(intList.isEmpty())

    val longList = mutableListOf<Long>()
    for (i in 2L until Long.MIN_VALUE step 2L) {
        longList += i
    }
    assertTrue(longList.isEmpty())

    val charList = mutableListOf<Char>()
    for (i in 'b' until Char.MIN_VALUE step 2) {
        charList += i
    }
    assertTrue(charList.isEmpty())

    return "OK"
}