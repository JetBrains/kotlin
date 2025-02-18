// FILE: 1.kt
open class A {
    fun a() = "OK"
}

inline fun foo(): String {
    class B : A()
    return B().a()
}

// FILE: 2.kt
fun box() = foo()