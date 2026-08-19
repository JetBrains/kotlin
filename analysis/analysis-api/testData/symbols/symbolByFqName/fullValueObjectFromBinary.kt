// class: pack/FullValueObjectFromBinary
// LANGUAGE: +FullValueClasses

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

value object FullValueObjectFromBinary

// MODULE: main(library)
// FILE: main.kt
import pack.FullValueObjectFromBinary

fun consume(value: FullValueObjectFromBinary) {}
