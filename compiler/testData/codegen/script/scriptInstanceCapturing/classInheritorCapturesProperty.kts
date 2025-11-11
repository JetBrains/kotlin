// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI

// expected: rv: OK

// KT-75589
val foo = "OK"

open class A {
    fun bar() = foo
}

class B : A()

val rv = B().bar()
