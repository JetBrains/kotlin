// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 1 until 7
        for (i in intProgression step 0 step 2) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 1L until 7L
        for (i in longProgression step 0L step 2L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'a' until 'g'
        for (i in charProgression step 0 step 2) {
        }
    }

    return "OK"
}