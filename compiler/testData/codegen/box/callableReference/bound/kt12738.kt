// IGNORE_BACKEND_FIR: JVM_IR
fun <T> get(t: T): () -> String {
    return t::toString
}

fun box(): String {
    if (get(null).invoke() != "null") return "Fail null"

    return get("OK").invoke()
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
