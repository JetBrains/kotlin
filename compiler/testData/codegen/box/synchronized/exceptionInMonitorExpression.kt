// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

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
