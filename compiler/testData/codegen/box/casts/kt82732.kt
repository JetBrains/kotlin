// WITH_STDLIB
// Ignored because of KT-83025
// IGNORE_BACKEND: WASM_JS, WASM_WASI

fun <T> List<T>.foo(): T {
    return this[0]
}

fun good(onBack: () -> Any?) {
    onBack()
}

fun bad(onBack: () -> Unit) {
    onBack()
}

fun box(): String {
    val j = listOf("a", "b")
    good(onBack = j::foo)
    val l = listOf("a", "b")
    bad(onBack = l::foo)
    return "OK"
}
