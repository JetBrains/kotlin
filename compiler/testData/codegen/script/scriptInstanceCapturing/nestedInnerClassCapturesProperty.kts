// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

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
