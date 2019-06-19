// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 downTo 2 step 2) {
        intList += i
    }
    assertTrue(intList.isEmpty())

    val longList = mutableListOf<Long>()
    for (i in 1L downTo 2L step 2L) {
        longList += i
    }
    assertTrue(longList.isEmpty())

    val charList = mutableListOf<Char>()
    for (i in 'a' downTo 'b' step 2) {
        charList += i
    }
    assertTrue(charList.isEmpty())

    return "OK"
}