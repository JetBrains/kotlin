// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var s = ""

    fun foo(x: String) {
        s += x
    }

    fun foo() {
        foo("K")
    }

    run {
        foo("O")
        foo()
    }

    return s
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
