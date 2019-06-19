// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun zero() = 0

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 1..7
        for (i in intProgression step zero()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 1L..7L
        for (i in longProgression step zero().toLong()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'a'..'g'
        for (i in charProgression step zero()) {
        }
    }

    return "OK"
}