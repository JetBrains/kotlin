// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// callable: one/foo
// LANGUAGE: +ContextReceivers
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

context(Int, String)
val foo: Boolean get() = false

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
