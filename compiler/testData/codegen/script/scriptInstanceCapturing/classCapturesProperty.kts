// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM

// expected: rv: abc

// KT-19423
val used = "abc"
class User {
    val property = used // error: Expression is inaccessible from a nested class
}

val rv = User().property
