package kotlin.test

class TestFailedException(val msg:String):RuntimeException(msg)

fun <T> assertEquals(a:T, b:T, msg:String = "") { if (a != b) throw TestFailedException(msg) }

/** Asserts that the expression is `true` with an optional [message]. */
fun assertTrue(actual: Boolean, message: String? = null) {
    if (actual != true) {
        throw TestFailedException(message ?: "Expected value to be true.")
    }
}

/** Asserts that the expression is `false` with an optional [message]. */
fun assertFalse(actual: Boolean, message: String? = null) {
    assertTrue(!actual, message ?: "Expected value to be false.")
}
