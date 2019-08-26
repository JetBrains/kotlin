// IGNORE_BACKEND: WASM
fun <T> test(a: T, b: T, operation: (x: T) -> T) {
    operation(if (3 > 2) a else b)
}

fun box(): String {
    test(1, 1, { it })
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
