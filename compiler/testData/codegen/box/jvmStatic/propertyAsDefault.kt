// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

object X {
    @JvmStatic val x = "OK"

    fun fn(value : String = x): String = value
}

fun box(): String {
    return X.fn()
}
