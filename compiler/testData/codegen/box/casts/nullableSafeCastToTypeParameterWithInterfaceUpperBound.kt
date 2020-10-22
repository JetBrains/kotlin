// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: NULLABLE_BOX_FUNCTION
interface I

fun <E: I> foo(a: Any?): E? = a as? E

fun box() = foo<I>(null) ?: "OK"