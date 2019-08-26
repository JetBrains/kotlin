// IGNORE_BACKEND: WASM
fun box(): String {
    val x = 2
    return when(x) {
        in (1..3) -> "OK"
        else -> "fail"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
