// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box() : String {
    try {
        return "OK"
    }
    finally {
        null?.toString()
    }
}
