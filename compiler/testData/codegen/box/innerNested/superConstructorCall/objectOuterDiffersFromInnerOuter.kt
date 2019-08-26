// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun bar(): Any {
        return {
            {
                object : Inner() {
                    override fun toString() = foo()
                }
            }()
        }()
    }

    open inner class Inner
    fun foo() = "OK"
}

fun box(): String = A().bar().toString()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
