// callable: test/foo
// KT-91580
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_DISTINCT_SYMBOL_POINTERS
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package test

fun foo(vararg x: Int) {}

@JvmName("fooArray")
fun foo(x: IntArray) {}

fun foo(vararg x: Int, y: IntArray) {}

@JvmName("fooArrayAndVararg")
fun foo(x: IntArray, vararg y: Int) {}

// MODULE: main(library)
// FILE: main.kt
