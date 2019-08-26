// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    var a = 1

    object {
        val t = run { a++ }
    }
    return if (a == 2) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
