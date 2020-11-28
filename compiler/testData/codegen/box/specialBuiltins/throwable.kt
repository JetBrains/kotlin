// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    try {
        throw Throwable("OK", null)
    } catch (t: Throwable) {
        if (t.cause != null) return "fail 1"
        return t.message!!
    }

    return "fail 2"
}
