// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 1..7
        for (i in intProgression step 0) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 1L..7L
        for (i in longProgression step 0L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'a'..'g'
        for (i in charProgression step 0) {
        }
    }

    return "OK"
}