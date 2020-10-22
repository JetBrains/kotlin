// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun foo(args: Array<String>) {
    try {
    } finally {
        try {
        } catch (e: Throwable) {
        }
    }
}

fun box() = "OK"
