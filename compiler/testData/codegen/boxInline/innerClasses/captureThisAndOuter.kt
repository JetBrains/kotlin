// FILE: 1.kt
open class A {
    fun a() = "OK"
}

class B : A()

inline fun foo(): String {
    return B().a()
}

// FILE: 2.kt
fun box() = foo()