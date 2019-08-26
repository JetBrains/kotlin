fun <T> block(block: () -> T): T = block()
fun foo() {}

fun test(): () -> Unit = block { fun() = foo() }

fun box(): String {
    test()
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
