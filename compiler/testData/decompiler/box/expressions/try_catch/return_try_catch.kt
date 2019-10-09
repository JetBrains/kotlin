fun fail(message: String): Nothing {
    throw IllegalArgumentException(message)
}

fun box(): String =
    try {
        val x = fail("Testing fail")
        "FAIL"
    } catch (e: IllegalArgumentException) {
        "OK"
    } finally {
        "OK"
    }