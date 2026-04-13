// ISSUE: KT-74777
// TARGET_PLATFORM: JS
// callable: one/Anno.value
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package one

annotation class Anno(vararg val value: Int = [0])

// MODULE: main(library)
// FILE: main.kt
fun main() {
}
