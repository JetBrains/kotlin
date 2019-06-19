// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun zero() = 0

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val intProgression = 7 downTo 1
        for (i in intProgression step zero()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val longProgression = 7L downTo 1L
        for (i in longProgression step zero().toLong()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        val charProgression = 'g' downTo 'a'
        for (i in charProgression step zero()) {
        }
    }

    return "OK"
}