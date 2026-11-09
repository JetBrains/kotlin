// class: pack/ExpectInlineValueClassWithNoConstructorFromCommonBinary
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +FullValueClasses
// TARGET_PLATFORM: Common

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

expect inline value class ExpectInlineValueClassWithNoConstructorFromCommonBinary

// MODULE: main(library)
// FILE: main.kt
import pack.ExpectInlineValueClassWithNoConstructorFromCommonBinary

fun consume(value: ExpectInlineValueClassWithNoConstructorFromCommonBinary) {}
