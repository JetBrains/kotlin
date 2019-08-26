// IGNORE_BACKEND: WASM
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    return when(val foo = 42UL) {
        42UL -> "OK"
        else -> "Fail"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNSIGNED