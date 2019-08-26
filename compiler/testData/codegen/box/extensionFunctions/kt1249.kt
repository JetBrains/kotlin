//KT-1249 IllegalStateException invoking function property
class TestClass(val body : () -> Unit) : Any() {
    fun run() {
        body()
    }
}

fun box() : String {
    TestClass({}).run()
    return "OK"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
