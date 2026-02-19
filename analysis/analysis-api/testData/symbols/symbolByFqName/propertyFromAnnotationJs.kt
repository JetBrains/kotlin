// ISSUE: KT-74777
// TARGET_PLATFORM: JS
// IGNORE_FE10
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
