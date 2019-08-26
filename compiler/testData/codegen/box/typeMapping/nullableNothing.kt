// IGNORE_BACKEND: WASM

var result = "fail"

fun sideEffect(): Any {
    result = "OK"
    return Unit
}

fun box(): String {
    // This used to be problematic because of an attempt to load kotlin/Nothing class
    val x = sideEffect() is Nothing?
    return result
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT