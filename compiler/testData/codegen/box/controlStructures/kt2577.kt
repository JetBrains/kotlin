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

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
