// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: 42

class C {
    fun foo() = B().bar()
}

val life = 42

class A {
    val x = life
}

class B {
    fun bar() = A().x
}

val rv = C().foo()
