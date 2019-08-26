// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val fn: () -> String)

interface Host {
    companion object : Base(run { { Host.ok() } }) {
        fun ok() = "OK"
    }
}

fun box() = Host.Companion.fn()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
