// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// JVM_ABI_K1_K2_DIFF: KT-63984
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

class D {
    var foo = 1
        private set

    fun foo() {
        foo = 2
    }
}

fun box(): String {
   D().foo()
   return "OK"
}
