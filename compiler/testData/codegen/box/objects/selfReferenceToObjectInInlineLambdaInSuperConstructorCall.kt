// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val fn: () -> String)

object Test : Base(run { { Test.ok() } }) {
    fun ok() = "OK"
}

fun box() = Test.fn()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
