// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in (8 downTo 1).reversed() step 2) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7), intList)

    val longList = mutableListOf<Long>()
    for (i in (8L downTo 1L).reversed() step 2L) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L), longList)

    val charList = mutableListOf<Char>()
    for (i in ('h' downTo 'a').reversed() step 2) {
        charList += i
    }
    assertEquals(listOf('a', 'c', 'e', 'g'), charList)

    return "OK"
}