// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM
// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: OK

// KT-75589
val foo = "OK"

open class A {
    fun bar() = foo
}

class B : A()

val rv = B().bar()
