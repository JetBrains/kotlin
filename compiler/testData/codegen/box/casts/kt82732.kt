// ISSUE: KT-82732
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3
// ^^^ K/Wasm backend v.2.3.0 has issue KT-82732, fixed only in 2.3.20-Beta2. So, a test `current frontend + 2.3.0 backend` expectedly fails
// WITH_STDLIB

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
