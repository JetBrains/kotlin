// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class En {
    A,
    B
}

fun box(): String = when(En.A) {
    En.A -> "OK"
    En.B -> "Fail 1"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
