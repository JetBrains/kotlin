// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in ((10 downTo 1 step 2).reversed() step 3).reversed()) {
        intList += i
    }
    assertEquals(listOf(8, 5, 2), intList)

    val longList = mutableListOf<Long>()
    for (i in ((10L downTo 1L step 2L).reversed() step 3L).reversed()) {
        longList += i
    }
    assertEquals(listOf(8L, 5L, 2L), longList)

    val charList = mutableListOf<Char>()
    for (i in (('j' downTo 'a' step 2).reversed() step 3).reversed()) {
        charList += i
    }
    assertEquals(listOf('h', 'e', 'b'), charList)

    return "OK"
}