// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun box(): String {
    val obj = "" as java.lang.Object
    val e = IllegalArgumentException()
    fun m(): Nothing = throw e
    try {
        synchronized (m()) {
            throw AssertionError("Should not have reached this point")
        }
    }
    catch (caught: Throwable) {
        if (caught !== e) return "Fail: $caught"
    }

    return "OK"
}
