// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i

fun box(): String = with(A(object : I {})) { "OK".id }

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ with 
