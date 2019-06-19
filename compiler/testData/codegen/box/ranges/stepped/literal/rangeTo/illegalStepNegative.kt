// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

// Bug in JS: Translation of loop over literal completely removes the validation of step
// DONT_TARGET_EXACT_BACKEND: JS
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1..7 step -1) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L..7L step -1L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a'..'g' step -1) {
        }
    }

    return "OK"
}