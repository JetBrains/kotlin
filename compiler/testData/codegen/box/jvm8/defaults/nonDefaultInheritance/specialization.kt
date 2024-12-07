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

interface Foo2<T> {
    fun foo(p: T): T = null!!
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt
class DerivedClass : Foo<String>

interface DerivedInterface<T> : Foo2<T> {
    override fun foo(p: T) = p
}

class DerivedClassWithSpecialization : DerivedInterface<String>

fun box(): String {
    return DerivedClass().foo("O") + DerivedClassWithSpecialization().foo("K")
}
