fun box(): String {
    try {
    } catch (e: Exception) {
        inlineFunctionWithDefaultArguments(e)
    }
    return "OK"
}

inline fun inlineFunctionWithDefaultArguments(t: Throwable? = null, bug: Boolean = true) =
        Unit