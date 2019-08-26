// IGNORE_BACKEND: WASM
fun box(): String {
    var s = ""
    try {
        throw RuntimeException()
    } catch (e : RuntimeException) {
    } finally {
        s += "OK"
    }
    return s
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ RuntimeException 
