// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI

// expected: rv: abc

// KT-19423
val used = "abc"
class User {
    val property = used
}

val rv = User().property
