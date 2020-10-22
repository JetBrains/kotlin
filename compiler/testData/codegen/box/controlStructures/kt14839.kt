// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    try {
    } catch (e: Exception) {
        inlineFunctionWithDefaultArguments(e)
    }
    return "OK"
}

inline fun inlineFunctionWithDefaultArguments(t: Throwable? = null, bug: Boolean = true) =
        Unit