// DONT_TARGET_EXACT_BACKEND: WASM
// WITH_RUNTIME

fun foo(fn: () -> Boolean) {}

fun box(): String {
    foo { 1 in setOf("1") }
    val a = 1 in setOf("1")
    return "OK"
}
