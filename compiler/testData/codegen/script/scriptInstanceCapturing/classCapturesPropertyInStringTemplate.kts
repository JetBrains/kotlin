// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR

// expected: rv: 42

fun foo() = B().bar()

val life = 42

class A {
    val x = "$life"
}

class B {
    fun bar() = A().x
}

val rv = foo()
