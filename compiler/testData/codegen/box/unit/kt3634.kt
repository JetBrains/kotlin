val c = Unit
val d = c

fun box(): String {
    c
    d
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT