// IGNORE_BACKEND: WASM
fun box(): String {
    var s = "OK"
    for (i in 1..3) {
        s = s + if (i<2) "" else continue
    }
    return s
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
