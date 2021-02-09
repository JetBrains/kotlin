// FILE: 1.kt
package test

class A {
    inline fun a() = B().b()
    inline fun c() = B().d()
}

class B {
    inline fun b() = A().c()
    inline fun d() = "OK"
}

// FILE: 2.kt
import test.*

fun box() = A().a()
