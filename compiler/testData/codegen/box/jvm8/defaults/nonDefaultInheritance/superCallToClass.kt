// TARGET_BACKEND: JVM
// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: A.kt

interface A<T> {
    fun f(p: T): T = p
}

abstract class B<T> : A<T>

// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// FILE: main.kt
abstract class C : B<String>()

class D : C() {
    fun g(): String = super.f("OK")
}

fun box(): String {
    return D().g()
}
