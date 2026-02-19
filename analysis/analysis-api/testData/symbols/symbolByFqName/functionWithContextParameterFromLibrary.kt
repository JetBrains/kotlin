// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// callable: one/foo
// LANGUAGE: +ContextParameters
// KT-75318
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

context(a: Int, b: String)
fun foo() {

}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
