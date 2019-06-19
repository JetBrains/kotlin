// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 until 2 step 2) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L until 2L step 2L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' until 'b' step 2) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}