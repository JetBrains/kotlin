fun box(): String {
    return if ((arrayOf(1, 2, 3)::get)(1) == 2) "OK" else "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
