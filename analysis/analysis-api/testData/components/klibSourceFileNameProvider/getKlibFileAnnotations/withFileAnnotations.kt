// MODULE: library
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary

// FILE: lib.kt
@file:Suppress("UNUSED_PARAMETER")

package test

class Baz
fun foo(x: Int) = 42
val bar = "hello"

// MODULE: main(library)
