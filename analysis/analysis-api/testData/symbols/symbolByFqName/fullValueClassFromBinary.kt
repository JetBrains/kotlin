// class: pack/FullValueClassFromBinary
// LANGUAGE: +FullValueClasses

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

value class FullValueClassFromBinary(val first: String, private val second: Int)

// MODULE: main(library)
// FILE: main.kt
fun consume(value: FullValueClassFromBinary) {}
