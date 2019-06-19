// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = Int.MIN_VALUE until Int.MAX_VALUE
    for (i in intProgression step Int.MAX_VALUE) {
        intList += i
    }
    assertEquals(listOf(Int.MIN_VALUE, -1, Int.MAX_VALUE - 1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = Long.MIN_VALUE until Long.MAX_VALUE
    for (i in longProgression step Long.MAX_VALUE) {
        longList += i
    }
    assertEquals(listOf(Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1), longList)

    return "OK"
}