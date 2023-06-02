// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR

// expected: rv: kotlin.Unit

fun foo() {
    B()
}
val b = B()

class A
fun A.ext() = Unit

class B {
    fun bar() {
        A().ext()
    }
}

val rv = foo()
