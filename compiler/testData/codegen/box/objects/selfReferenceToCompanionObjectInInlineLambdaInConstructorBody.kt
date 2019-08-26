// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    companion object {
        fun ok() = "OK"
        val x = run { Test.ok() }
        fun test() = x
    }
}

fun box() = Test.test()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
