// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM

// expected: rv: abcabc

// KT-19423 variation
val used = "abc"

inner class Outer {
    val middle = used
    inner class User {
        val property = used
    }
}

val rv = Outer().User().property + Outer().middle
