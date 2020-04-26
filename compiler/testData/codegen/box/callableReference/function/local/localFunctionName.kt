fun box(): String {
    fun OK() {}

    return ::OK.name
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
