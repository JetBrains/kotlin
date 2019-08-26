fun foo(block: (String, String, String) -> String): String = block("O", "fail", "K")

fun box() = foo(fun(x: String, _: String, y: String) = x + y)

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
