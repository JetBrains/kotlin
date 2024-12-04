// NATIVE error: static cache is broken: ld.gold invocation reported errors. Please try to disable compiler caches and rerun the build.
// DONT_TARGET_EXACT_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// KT-65416
// IGNORE_BACKEND_K2: WASM


// MODULE: lib
// FILE: 2.kt
abstract class A {
    private val value = "OK"
    fun f() = value
}

abstract class B : A()

// FILE: 3.kt
abstract class C : B()

// MODULE: main(lib)
// FILE: 1.kt
class D : C()

fun box(): String = D().f()

// FILE: 2.kt
abstract class A {
    private val value = "OK"
    fun f() = value
}

abstract class B : A()
