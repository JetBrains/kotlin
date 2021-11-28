fun test(): String {
    var exception: Throwable? = null
    val f = {
        exception = IllegalStateException("OK")
    }
    f()

    if (exception != null) {
        throw exception!!
    } else {
        return "Fail"
    }
}

fun box(): String = try {
    test()
} catch (e: IllegalStateException) {
    e.message!!
}
