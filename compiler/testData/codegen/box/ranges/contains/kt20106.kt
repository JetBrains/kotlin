// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val strSet = setOf("a", "b")
    val xx = "a" to ("a" in strSet)
    return if (!xx.second) "fail" else "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: STDLIB_HASH_SET
