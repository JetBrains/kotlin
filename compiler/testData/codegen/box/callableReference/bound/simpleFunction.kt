fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
