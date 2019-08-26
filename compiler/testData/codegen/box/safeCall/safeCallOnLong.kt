// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun f(b : Long.(Long)->Long) = 1L?.b(2L)

fun box(): String {
    val x = f { this + it }
    return if (x == 3L) "OK" else "fail $x"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
