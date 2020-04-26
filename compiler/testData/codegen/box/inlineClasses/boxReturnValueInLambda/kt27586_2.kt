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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY