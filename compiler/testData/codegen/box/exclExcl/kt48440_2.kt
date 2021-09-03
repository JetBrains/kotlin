fun <T> id(t: T): T = t

fun test(b: Boolean): String {
    var exception: Throwable? = null
    if (b) {
        exception = IllegalStateException("OK")
    }

    if (exception != null) {
        throw id(exception)!!
    } else {
        return "Fail"
    }
}

fun box(): String = try {
    test(true)
} catch (e: IllegalStateException) {
    e.message!!
}
