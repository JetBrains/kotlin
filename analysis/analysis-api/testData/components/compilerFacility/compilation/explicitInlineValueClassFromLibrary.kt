// LANGUAGE: +FullValueClasses

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

inline value class ExplicitInlineValueClass(val value: Int) {
    val doubled: Int get() = value * 2
}

// MODULE: main(lib)
// FILE: main.kt
import test.ExplicitInlineValueClass

fun test(value: ExplicitInlineValueClass): ExplicitInlineValueClass = ExplicitInlineValueClass(value.doubled)
