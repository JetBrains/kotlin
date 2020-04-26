// WITH_RUNTIME
fun f1(): () -> Result<String> {
    return {
        runCatching {
            "OK"
        }
    }
}

fun box(): String {
    val r = f1()()
    return r.getOrNull() ?: "fail: $r"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY