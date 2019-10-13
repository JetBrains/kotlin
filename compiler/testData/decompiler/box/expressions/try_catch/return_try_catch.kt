fun fail(message: String): Nothing {
    throw IllegalArgumentException(message)
}

fun box(): String {
    return try {
        val x = fail("Testing fail")
        "FAIL"
    } catch (e: IllegalArgumentException) {
        "OK"
    } finally {
        "FAIL"
    }
}