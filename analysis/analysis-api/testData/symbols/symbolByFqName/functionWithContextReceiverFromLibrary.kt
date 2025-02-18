// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// callable: one/foo
// LANGUAGE: +ContextReceivers
// KT-75318
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

context(Int, String)
fun foo() {

}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
