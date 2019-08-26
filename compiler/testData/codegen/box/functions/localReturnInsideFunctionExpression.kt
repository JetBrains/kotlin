// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun simple() = fun (): Boolean { return true }

fun withLabel() = l@ fun (): Boolean { return@l true }

fun box(): String {
    if (!simple()()) return "Test simple failed"
    if (!withLabel()()) return "Test withLabel failed"

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
