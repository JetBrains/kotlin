// class: pack/FullValueClassFromCommonBinary
// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: Common

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

value class FullValueClassFromCommonBinary(val value: Int)

// MODULE: main(library)
// FILE: main.kt
fun consume(value: FullValueClassFromCommonBinary) {}
