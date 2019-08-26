// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo(x: String) = x
    fun foo() = foo("K")

    return run {
        foo("O") + foo()
    }
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
