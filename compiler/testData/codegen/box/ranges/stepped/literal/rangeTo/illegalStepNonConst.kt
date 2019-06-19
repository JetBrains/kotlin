// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

// Bug in JS: Translation of loop over literal completely removes the validation of step
// DONT_TARGET_EXACT_BACKEND: JS
import kotlin.test.*

fun zero() = 0

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1..7 step zero()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L..7L step zero().toLong()) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a'..'g' step zero()) {
        }
    }

    return "OK"
}