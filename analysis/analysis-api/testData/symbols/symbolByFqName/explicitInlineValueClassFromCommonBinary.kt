// class: pack/ExplicitInlineValueClassFromCommonBinary
// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: Common

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

inline value class ExplicitInlineValueClassFromCommonBinary(val value: Int)

// MODULE: main(library)
// FILE: main.kt
import pack.ExplicitInlineValueClassFromCommonBinary

fun consume(value: ExplicitInlineValueClassFromCommonBinary) {}
