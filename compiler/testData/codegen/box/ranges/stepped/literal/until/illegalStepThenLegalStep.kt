// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1 until 7 step 0 step 2) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L until 7L step 0L step 2L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a' until 'g' step 0 step 2) {
        }
    }

    return "OK"
}