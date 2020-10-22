// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
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
