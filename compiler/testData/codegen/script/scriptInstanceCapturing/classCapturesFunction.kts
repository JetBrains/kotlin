// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM

// expected: rv: 42

val x = 6
val y = 7

fun foo() = x

class A {
    fun bar() = foo() * y
}

val rv = A().bar()
