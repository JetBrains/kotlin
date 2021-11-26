// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// WITH_STDLIB

fun box(): String {
    val list = arrayOf("a", "c", "b").sorted()
    return if (list.toString() == "[a, b, c]") "OK" else "Fail: $list"
}
