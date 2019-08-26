// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun test() = foo({ line: String -> line })

fun <T> foo(x: T): T = TODO()

fun box(): String {
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ TODO 
