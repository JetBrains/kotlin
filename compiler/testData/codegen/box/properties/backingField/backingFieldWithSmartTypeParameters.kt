// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS
// TARGET_BACKEND: JS_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS, JS_ES6, WASM
// WITH_STDLIB

val items: List<String>
    field = mutableListOf()

fun box(): String {
    items.add("OK")
    return items.last()
}
