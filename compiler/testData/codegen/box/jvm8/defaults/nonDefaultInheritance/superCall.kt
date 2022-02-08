// CHECK_BYTECODE_LISTING
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: NCDFE: Foo$DefaultImpls
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// !JVM_DEFAULT_MODE: all
// FILE: Foo.kt

interface Foo<T> {
    fun foo(p: T) = p
}

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: disable
// FILE: main.kt
interface DerivedInterface<T> : Foo<T>

class DerivedClass : DerivedInterface<String> {
    override fun foo(p: String) = super.foo(p)
}

fun box(): String {
    return DerivedClass().foo("OK")
}
