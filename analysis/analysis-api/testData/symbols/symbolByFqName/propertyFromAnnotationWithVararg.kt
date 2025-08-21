// ISSUE: KT-74777
// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

annotation class Anno(vararg val value: Int)

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
