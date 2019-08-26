class T(val f : () -> Any?) {
    fun call() : Any? = f()
}
fun box(): String {
    return T({ "OK" }).call() as String
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
