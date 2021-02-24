// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val strSet = setOf("a", "b")
    val xx = "a" to ("a" in strSet)
    return if (!xx.second) "fail" else "OK"
}