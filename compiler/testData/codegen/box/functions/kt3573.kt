// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class Data

fun newInit(f: Data.() -> Data) = Data().f()

class TestClass {
    val test: Data = newInit()  { this }
}

fun box() : String {
    TestClass()
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
