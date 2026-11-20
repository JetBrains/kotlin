// callable: test/C.foo
// KT-91552
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package test

interface A
interface B

class C {
    fun <T : A> foo(x: T) {}
    fun <T : B> foo(x: T) {}

    val <T : A> T.foo: Int get() = 0
    val <T : B> T.foo: Int get() = 0
}

// MODULE: main(library)
// FILE: main.kt
