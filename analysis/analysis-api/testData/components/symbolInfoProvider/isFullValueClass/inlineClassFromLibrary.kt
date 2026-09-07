// WITH_STDLIB

// MODULE: lib
// MODULE_KIND: LibraryBinary
// LANGUAGE: -FullValueClasses
// FILE: lib.kt
@JvmInline
value class UserId(val value: Int)

// MODULE: main(lib)
// LANGUAGE: +FullValueClasses
// FILE: main.kt
// class: /UserId
