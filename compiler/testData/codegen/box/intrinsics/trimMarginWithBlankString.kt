// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT

fun box(): String {
    try {
        "a b c".trimMargin(" ")
        return "Fail trimMargin"
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
}
