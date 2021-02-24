// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    try {
        "a b c".trimMargin(" ")
        return "Fail trimMargin"
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
}
