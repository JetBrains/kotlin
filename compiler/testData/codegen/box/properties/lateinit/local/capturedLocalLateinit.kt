// IGNORE_BACKEND: WASM
fun runNoInline(f: () -> Unit) = f()

fun box(): String {
    lateinit var ok: String
    runNoInline {
        ok = "OK"
    }
    return ok
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
