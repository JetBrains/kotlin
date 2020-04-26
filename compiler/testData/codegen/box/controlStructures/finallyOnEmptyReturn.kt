var result = "Fail"

fun foo() {
    try {
        return
    } finally {
        result = "OK"
    }
}

fun box(): String {
    foo()
    return result
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
