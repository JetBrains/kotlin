interface I

fun <E: I> foo(a: Any?): E? = a as? E

fun box() = foo<I>(null) ?: "OK"

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: MINOR: NULLABLE_BOX_FUNCTION
