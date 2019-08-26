// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
//KT-1061 Can't call function defined as a val

object X {
    val doit = { i: Int -> i }
}

fun box() : String = if (X.doit(3) == 3) "OK" else "fail"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
