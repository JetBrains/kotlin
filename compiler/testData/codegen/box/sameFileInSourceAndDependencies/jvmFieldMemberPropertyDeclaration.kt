// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: 2.kt
abstract class A {
    @JvmField val value: String = "OK"
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
    @JvmField val value: String = "OK"
    fun f() = value
}

abstract class B : A()
