// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1 until 11
    for (i in ((intProgression step 2).reversed() step 3).reversed()) {
        intList += i
    }
    assertEquals(listOf(3, 6, 9), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L until 11L
    for (i in ((longProgression step 2L).reversed() step 3L).reversed()) {
        longList += i
    }
    assertEquals(listOf(3L, 6L, 9L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a' until 'k'
    for (i in ((charProgression step 2).reversed() step 3).reversed()) {
        charList += i
    }
    assertEquals(listOf('c', 'f', 'i'), charList)

    return "OK"
}