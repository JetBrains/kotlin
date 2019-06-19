// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    for (i in ((1 until 9).reversed() step 2).reversed()) {
        intList += i
    }
    assertEquals(listOf(2, 4, 6, 8), intList)

    val longList = mutableListOf<Long>()
    for (i in ((1L until 9L).reversed() step 2L).reversed()) {
        longList += i
    }
    assertEquals(listOf(2L, 4L, 6L, 8L), longList)

    val charList = mutableListOf<Char>()
    for (i in (('a' until 'i').reversed() step 2).reversed()) {
        charList += i
    }
    assertEquals(listOf('b', 'd', 'f', 'h'), charList)

    return "OK"
}