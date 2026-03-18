// ISSUE: KT-74777
// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

annotation class Anno(val value: Int)

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
