// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class C {
    companion object {
        private val s: String
        private var s2: String

        init {
            s = "O"
            s2 = "O"
        }

        fun foo() = s

        fun foo2() = s2

        fun bar2() { s2 = "K" }
    }
}

fun box(): String {
    return C.foo() + {C.bar2(); C.foo2()}()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
