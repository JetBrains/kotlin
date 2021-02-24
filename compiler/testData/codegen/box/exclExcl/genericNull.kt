// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun <T> foo(t: T) {
    t!!
}

fun box(): String {
    try {
        foo<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
