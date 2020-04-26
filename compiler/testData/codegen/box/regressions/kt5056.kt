// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val list = arrayOf("a", "c", "b").sorted()
    return if (list.toString() == "[a, b, c]") "OK" else "Fail: $list"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_GENERATED
