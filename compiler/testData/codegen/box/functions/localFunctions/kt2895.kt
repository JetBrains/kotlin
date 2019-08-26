// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun outer() {
    fun inner(i: Int) {
        if (i > 0){
            {
                it: Int -> inner(0) // <- invocation of literal itself is generated instead
            }.invoke(1)
        }
    }
    inner(1)
}

fun box(): String {
    outer()
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
