// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI

// expected: rv: 42

val x = 6
val y = 7

fun foo() = x

class A {
    fun bar() = foo() * y
}

val rv = A().bar()
