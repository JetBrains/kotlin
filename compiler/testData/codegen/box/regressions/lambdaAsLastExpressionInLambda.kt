// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
val foo: ((String) -> String) = run {
    { it }
}

fun box() = foo("OK")
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
