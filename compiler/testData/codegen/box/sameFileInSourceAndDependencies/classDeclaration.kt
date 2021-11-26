// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
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
