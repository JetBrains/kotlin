// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
lateinit var ok: String

fun box(): String {
    run {
        ok = "OK"
    }
    return ok
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
