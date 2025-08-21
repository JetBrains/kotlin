// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// JVM_ABI_K1_K2_DIFF: KT-63984

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
