// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI

// expected: rv: abc!

// KT-19423 variation
val used = "abc"

class Outer {
    val bang = "!"
    inner class User {
        val property = used + bang
    }
}

val rv = Outer().User().property
