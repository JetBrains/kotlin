// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun <T> castToString(t: T) {
    t as String
}


fun box(): String {
    try {
        castToString<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
