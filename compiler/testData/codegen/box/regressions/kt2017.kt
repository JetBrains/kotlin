// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_SORT
// WITH_STDLIB

fun box(): String {
    val sorted = arrayListOf("1", "3", "2").sorted()
    return if (sorted != arrayListOf("1", "2", "3")) "$sorted" else "OK"
}
