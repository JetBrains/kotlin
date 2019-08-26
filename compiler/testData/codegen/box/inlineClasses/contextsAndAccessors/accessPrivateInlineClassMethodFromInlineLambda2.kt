// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

inline class R(private val r: Long) {
    fun test() = run { ok() }

    private fun ok() = "OK"
}

fun box() = R(0).test()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
