// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 7 downTo 1 step 7) {
        intList += i
    }
    assertEquals(listOf(7), intList)

    val longList = mutableListOf<Long>()
    for (i in 7L downTo 1L step 7L) {
        longList += i
    }
    assertEquals(listOf(7L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'g' downTo 'a' step 7) {
        charList += i
    }
    assertEquals(listOf('g'), charList)

    return "OK"
}