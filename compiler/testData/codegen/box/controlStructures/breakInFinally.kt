// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String {
    while (true) {
        try {
            continue;
        }
        finally {
            break;
        }
    }
    return "OK"
}