// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI
// IGNORE_BACKEND_K2: JVM_IR

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
