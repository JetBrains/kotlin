// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// WITH_RUNTIME
fun f1() = lazy {
    runCatching {
        "OK"
    }
}

fun box(): String {
    val r = f1().value
    return r.getOrNull() ?: "fail: $r"
}