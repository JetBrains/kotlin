// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

class A {
    val a: Number
        private field = 1

    val b: Number
        internal field = a + 3
}

fun box(): String {
    return if (A().b + 20 == 24) {
        "OK"
    } else {
        "fail: A().b = " + A().b.toString()
    }
}
