// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 7.toByte() downTo 1.toShort()
    for (i in intProgression step 2) {
        intList += i
    }
    assertEquals(listOf(7, 5, 3, 1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 7 downTo 1L
    for (i in longProgression step 2) {
        longList += i
    }
    assertEquals(listOf(7L, 5L, 3L, 1L), longList)

    return "OK"
}