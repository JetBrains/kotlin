// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

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
