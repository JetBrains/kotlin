// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
val Int.getter: Int
    get() {
        return {
            this@getter
        }.invoke()
    }

fun box(): String {
    val i = 1
    if (i.getter != 1) return "getter failed"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
