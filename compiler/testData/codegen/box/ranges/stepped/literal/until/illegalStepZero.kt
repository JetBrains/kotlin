// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

// Bug in JS: Translation of loop over literal completely removes the validation of step
// DONT_TARGET_EXACT_BACKEND: JS
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1 until 7 step 0) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L until 7L step 0L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a' until 'g' step 0) {
        }
    }

    return "OK"
}