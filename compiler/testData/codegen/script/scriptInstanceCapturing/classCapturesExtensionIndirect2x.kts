// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM

// expected: rv: kotlin.Unit

class C {
    fun foo() {
        B()
    }
}

class A
fun A.ext() = Unit

class B {
    fun bar() {
        A().ext()
    }
}

val rv = C().foo()
