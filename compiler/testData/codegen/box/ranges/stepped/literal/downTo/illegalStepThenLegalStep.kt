// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        for (i in 7 downTo 1 step 0 step 2) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 7L downTo 1L step 0L step 2L) {
        }
    }

    assertFailsWith<IllegalArgumentException> {
        for (i in 'g' downTo 'a' step 0 step 2) {
        }
    }

    return "OK"
}