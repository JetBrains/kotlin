// MODULE: lib
// FILE: lib.kt
interface A {
    fun foo(): String
}

abstract class B(a: A) : A by a

// MODULE: main(lib)
// FILE: main.kt
class AImpl : A {
    override fun foo(): String = "OK"
}
class C : B(AImpl())

fun box(): String = C().foo()
