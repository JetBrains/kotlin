// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
fun <T> get(t: T): () -> String {
    return t::toString
}

fun box(): String {
    if (get(null).invoke() != "null") return "Fail null"

    return get("OK").invoke()
}
