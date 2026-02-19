// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

data class DataClass(val name: String)

// MODULE: main(lib)
// FILE: usage.kt
fun usage(instance: one.DataClass) {
    instance.<caret>toString()
}