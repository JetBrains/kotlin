// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in ((1 until 11).reversed() step 2).reversed() step 3) {
        intList += i
    }
    assertEquals(listOf(2, 5, 8), intList)

    val longList = mutableListOf<Long>()
    for (i in ((1L until 11L).reversed() step 2L).reversed() step 3L) {
        longList += i
    }
    assertEquals(listOf(2L, 5L, 8L), longList)

    val charList = mutableListOf<Char>()
    for (i in (('a' until 'k').reversed() step 2).reversed() step 3) {
        charList += i
    }
    assertEquals(listOf('b', 'e', 'h'), charList)

    return "OK"
}