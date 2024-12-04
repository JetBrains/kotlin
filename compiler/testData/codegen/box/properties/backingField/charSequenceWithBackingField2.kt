// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

class Base {
    val x: CharSequence
        internal field: String = "OK"

    val s: String get() = x
}

fun box(): String {
    return Base().s
}
