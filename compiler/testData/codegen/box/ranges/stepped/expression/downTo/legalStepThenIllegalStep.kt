// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 7 downTo 1
        for (i in intProgression step 2 step 0) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 7L downTo 1L
        for (i in longProgression step 2L step 0L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'g' downTo 'a'
        for (i in charProgression step 2 step 0) {
        }
    }

    return "OK"
}