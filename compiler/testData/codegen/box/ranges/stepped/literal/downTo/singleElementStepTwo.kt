// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 downTo 1 step 2) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L downTo 1L step 2L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' downTo 'a' step 2) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}