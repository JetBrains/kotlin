fun foo(args: Array<String>) {
    try {
    } finally {
        try {
        } catch (e: Throwable) {
        }
    }
}

fun box() = "OK"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
