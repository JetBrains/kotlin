// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    var x = "OK"
    while (true) {
        try {
            x = x + continue
        }
        finally {
            x = x + break
        }
    }
    return x
}
