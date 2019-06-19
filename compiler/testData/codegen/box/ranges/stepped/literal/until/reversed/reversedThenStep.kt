// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in (1 until 9).reversed() step 2) {
        intList += i
    }
    assertEquals(listOf(8, 6, 4, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in (1L until 9L).reversed() step 2L) {
        longList += i
    }
    assertEquals(listOf(8L, 6L, 4L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in ('a' until 'i').reversed() step 2) {
        charList += i
    }
    assertEquals(listOf('h', 'f', 'd', 'b'), charList)

    return "OK"
}