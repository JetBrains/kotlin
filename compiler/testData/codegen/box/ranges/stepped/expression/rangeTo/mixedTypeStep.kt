// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1.toShort()..7.toByte()
    for (i in intProgression step 2) {
        intList += i
    }
    assertEquals(listOf(1, 3, 5, 7), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..7
    for (i in longProgression step 2) {
        longList += i
    }
    assertEquals(listOf(1L, 3L, 5L, 7L), longList)

    return "OK"
}