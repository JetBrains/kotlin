// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun foo(): Int {
    try {
    } finally {
        try {
            return 1
        } catch (e: Throwable) {
            return 2
        }
    }
}

fun box() = if (foo() == 1) "OK" else "Fail"
