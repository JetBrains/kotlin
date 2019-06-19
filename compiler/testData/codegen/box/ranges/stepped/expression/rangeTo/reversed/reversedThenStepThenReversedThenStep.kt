// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..10
    for (i in (intProgression.reversed() step 2).reversed() step 3) {
        intList += i
    }
    assertEquals(listOf(2, 5, 8), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..10L
    for (i in (longProgression.reversed() step 2L).reversed() step 3L) {
        longList += i
    }
    assertEquals(listOf(2L, 5L, 8L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..'j'
    for (i in (charProgression.reversed() step 2).reversed() step 3) {
        charList += i
    }
    assertEquals(listOf('b', 'e', 'h'), charList)

    return "OK"
}