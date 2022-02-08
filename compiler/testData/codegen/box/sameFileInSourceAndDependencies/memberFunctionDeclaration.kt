// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: 2.kt
abstract class A {
    protected val value = "OK"
}

abstract class B : A() {
    fun f() = value
}

// FILE: 3.kt
abstract class C : B()

// MODULE: main(lib)
// FILE: 1.kt
class D : C()

fun box(): String = D().f()

// FILE: 2.kt
abstract class A {
    protected val value = "OK"
}

abstract class B : A() {
    fun f() = value
}
