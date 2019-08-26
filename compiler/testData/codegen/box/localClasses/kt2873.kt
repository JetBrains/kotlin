fun foo() : String {
    val u = {
        class B(val data : String)
        B("OK").data
    }
    return u()
}

fun box(): String {
    return foo()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
