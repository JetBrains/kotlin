// callable: test/foo
// KT-91552
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_DISTINCT_SYMBOL_POINTERS
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package test

interface A
interface B

fun <T : A> foo(x: T) {}
fun <T : B> foo(x: T) {}

fun <T : A, U : B> foo(x: T, y: U) {}
fun <U : A, T : B> foo(x: T, y: U) {}

val <T : A> T.foo: Int get() = 0
val <T : B> T.foo: Int get() = 0

// MODULE: main(library)
// FILE: main.kt
