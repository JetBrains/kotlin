// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..8
    for (i in intProgression.reversed() step 2) {
        intList += i
    }
    assertEquals(listOf(8, 6, 4, 2), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..8L
    for (i in longProgression.reversed() step 2L) {
        longList += i
    }
    assertEquals(listOf(8L, 6L, 4L, 2L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..'h'
    for (i in charProgression.reversed() step 2) {
        charList += i
    }
    assertEquals(listOf('h', 'f', 'd', 'b'), charList)

    return "OK"
}