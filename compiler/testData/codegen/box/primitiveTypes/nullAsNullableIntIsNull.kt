// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    try {
        if ((null as Int?)!! == 10) return "Fail #1"
        return "Fail #2"
    }
    catch (e: Exception) {
        return "OK"
    }
}
