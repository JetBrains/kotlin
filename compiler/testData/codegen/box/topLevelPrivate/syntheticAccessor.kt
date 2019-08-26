package test

private val prop = "O"

private fun test() = "K"

fun box(): String {
    return {
        prop + test()
    }()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
