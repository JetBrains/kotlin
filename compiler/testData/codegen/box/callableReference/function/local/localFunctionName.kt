// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
fun box(): String {
    fun OK() {}

    return ::OK.name
}
