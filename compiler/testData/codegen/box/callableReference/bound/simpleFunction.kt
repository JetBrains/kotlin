// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}
