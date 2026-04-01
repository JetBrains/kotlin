// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// callable: one/foo
// LANGUAGE: +ContextParameters
// KT-75318
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

context(a: Int, b: String)
val foo: Boolean get() = false

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
