// class: pack/ExplicitInlineValueClassFromBinary
// LANGUAGE: +FullValueClasses

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

inline value class ExplicitInlineValueClassFromBinary(val value: Int)

// MODULE: main(library)
// FILE: main.kt
import pack.ExplicitInlineValueClassFromBinary

fun consume(value: ExplicitInlineValueClassFromBinary) {}
