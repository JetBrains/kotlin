// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val sorted = arrayListOf("1", "3", "2").sorted()
    return if (sorted != arrayListOf("1", "2", "3")) "$sorted" else "OK"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
