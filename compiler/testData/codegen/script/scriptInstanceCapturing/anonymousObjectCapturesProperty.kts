// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM

// expected: rv: 42

fun foo() = B.bar()

val life = 42

interface A {
    fun bar(): Int
}

val B = object : A {
    override fun bar() = life
}

val rv = foo()
