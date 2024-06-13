// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM_IR

// expected: rv: <nofield>

// KT-30616
val foo = "hello"

class Bar(val s: String) {
    companion object {
        fun t() {
            Bar(foo)
        }
    }
}
