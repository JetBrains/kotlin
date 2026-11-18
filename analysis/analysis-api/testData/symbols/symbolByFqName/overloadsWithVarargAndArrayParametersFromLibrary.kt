// callable: test/foo
// KT-91580
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
