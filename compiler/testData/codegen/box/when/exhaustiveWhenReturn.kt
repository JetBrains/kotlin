// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class A { V }

fun box(): String {
    val a: A = A.V
    when (a) {
        A.V -> return "OK"
    }
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
