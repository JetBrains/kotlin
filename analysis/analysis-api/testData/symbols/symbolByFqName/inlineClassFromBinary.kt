// class: pack/InlineClassFromBinary
// WITH_STDLIB

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

@JvmInline
value class InlineClassFromBinary(val value: Int)

// MODULE: main(library)
// FILE: main.kt
import pack.InlineClassFromBinary

fun consume(value: InlineClassFromBinary) {}
