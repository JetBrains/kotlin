// IGNORE_BACKEND: JS, JS_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM, JVM_IR

// expected: rv: <nofield>

// KT-30616
val foo = "hello"

enum class Bar(val s: String) {
    Eleven(s = foo)
}
