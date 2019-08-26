// IGNORE_BACKEND: WASM
fun box(): String {
    var cycle = true;
    while (true) {
        if (true || throw RuntimeException()) {
            return "OK"
        }
    }
    return "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ RuntimeException 
