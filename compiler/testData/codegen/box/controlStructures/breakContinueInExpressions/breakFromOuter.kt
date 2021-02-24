// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    OUTER@while (true) {
        var x = ""
        try {
            do {
                x = x + break@OUTER
            } while (true)
        } finally {
            return "OK"
        }
    }
}