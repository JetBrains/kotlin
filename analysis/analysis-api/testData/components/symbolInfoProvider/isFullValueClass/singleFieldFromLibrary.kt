// MODULE: lib
// MODULE_KIND: LibraryBinary
// LANGUAGE: +FullValueClasses
// FILE: lib.kt
value class UserId(val value: Int)

// MODULE: main(lib)
// LANGUAGE: -FullValueClasses
// FILE: main.kt
// class: /UserId
