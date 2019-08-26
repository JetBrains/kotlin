// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A {
    public var prop = "OK"
        private set


    fun test(): String {
        return { prop }()
    }
}

fun box(): String = A().test()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
