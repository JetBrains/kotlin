// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS
// TARGET_BACKEND: JS_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS, JS_ES6, WASM

class Base {
    val x: CharSequence
        internal field: String = "OK"

}
val s: String = Base().x
fun box(): String {
    return s
}
