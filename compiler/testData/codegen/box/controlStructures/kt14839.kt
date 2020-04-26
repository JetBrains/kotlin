fun box(): String {
    try {
    } catch (e: Exception) {
        inlineFunctionWithDefaultArguments(e)
    }
    return "OK"
}

inline fun inlineFunctionWithDefaultArguments(t: Throwable? = null, bug: Boolean = true) =
        Unit
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
