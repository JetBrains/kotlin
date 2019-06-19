// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..8
    for (i in (intProgression.reversed() step 2).reversed()) {
        intList += i
    }
    assertEquals(listOf(2, 4, 6, 8), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..8L
    for (i in (longProgression.reversed() step 2L).reversed()) {
        longList += i
    }
    assertEquals(listOf(2L, 4L, 6L, 8L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..'h'
    for (i in (charProgression.reversed() step 2).reversed()) {
        charList += i
    }
    assertEquals(listOf('b', 'd', 'f', 'h'), charList)

    return "OK"
}