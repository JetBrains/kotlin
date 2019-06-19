// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in 1 until 8 step 7) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    for (i in 1L until 8L step 7L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    for (i in 'a' until 'h' step 7) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}