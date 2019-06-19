// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 1..7 step 2 step 0) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 1L..7L step 2L step 0L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'a'..'g' step 2 step 0) {
        }
    }

    return "OK"
}