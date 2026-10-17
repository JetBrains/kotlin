// class: pack/ExplicitInlineValueClassWithJvmInlineFromBinary
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

@JvmInline
inline value class ExplicitInlineValueClassWithJvmInlineFromBinary(val value: Int)

// MODULE: main(library)
// FILE: main.kt
import pack.ExplicitInlineValueClassWithJvmInlineFromBinary

fun consume(value: ExplicitInlineValueClassWithJvmInlineFromBinary) {}
