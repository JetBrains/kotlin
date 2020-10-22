// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    var s = ""
    try {
        throw RuntimeException()
    } catch (e : RuntimeException) {
    } finally {
        s += "OK"
    }
    return s
}
