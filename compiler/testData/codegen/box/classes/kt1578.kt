// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    var i = 0
        {
            i++
        }()
    i++  //the problem is here
//    i = i + 1  //this way it works
    return if (i == 2) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
