// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8

// FILE: Kotlin.kt
interface Foo<T> {
    fun foo(p: T): T = p
}

interface FooDerived: Foo<Derived>

class Derived(val value: String)

class Test : FooDerived {
    override fun foo(a: Derived): Derived {
        return super.foo(a)
    }
}

fun box(): String {
    return Test().foo(Derived("OK")).value
}
