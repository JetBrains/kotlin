// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun zero() = 0

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 1 until 7
        for (i in intProgression step zero()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 1L until 7L
        for (i in longProgression step zero().toLong()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'a' until 'g'
        for (i in charProgression step zero()) {
        }
    }

    return "OK"
}