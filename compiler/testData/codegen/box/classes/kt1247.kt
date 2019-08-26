// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun f(a : Int?, b : Int.(Int)->Int) = a?.b(1)

fun box(): String {
    val x = f(1) { this+it+2 }
    return if (x == 4) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
