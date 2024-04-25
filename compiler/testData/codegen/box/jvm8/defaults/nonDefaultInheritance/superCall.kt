// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// JVM_DEFAULT_MODE: all
// FILE: Foo.kt

interface Foo<T> {
    fun foo(p: T) = p
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt
interface DerivedInterface<T> : Foo<T>

class DerivedClass : DerivedInterface<String> {
    override fun foo(p: String) = super.foo(p)
}

fun box(): String {
    return DerivedClass().foo("OK")
}
