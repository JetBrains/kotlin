// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var s = ""
    var foo = "K"

    fun foo(x: String, y: Int) {
        s += x
    }

    fun test() {
        fun foo(x: String) {
            s += x
        }

        {
            foo("O")
            foo(foo, 1)
        }()
    }

    test()

    return s
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
