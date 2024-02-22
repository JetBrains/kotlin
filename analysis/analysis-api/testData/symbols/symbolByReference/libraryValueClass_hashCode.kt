// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

@JvmInline
value class ValueClass(val name: String)

// MODULE: main(lib)
// FILE: usage.kt
fun usage(instance: one.ValueClass) {
    instance.<caret>hashCode()
}