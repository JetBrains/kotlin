// FILE: main.kt
fun box(): String {
    try {
    } catch (e: Exception) {
        inlineFunctionWithDefaultArguments(e)
    }
    return "OK"
}

// FILE: lib.kt
inline fun inlineFunctionWithDefaultArguments(t: Throwable? = null, bug: Boolean = true) =
        Unit