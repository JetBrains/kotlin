// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

object X {
    @JvmStatic val x = "OK"

    fun fn(value : String = x): String = value
}

fun box(): String {
    return X.fn()
}
