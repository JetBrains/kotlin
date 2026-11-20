// class: pack/ExpectFullValueClassWithNoConstructorFromCommonBinary
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +FullValueClasses
// TARGET_PLATFORM: Common

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package pack

expect value class ExpectFullValueClassWithNoConstructorFromCommonBinary

// MODULE: main(library)
// FILE: main.kt
import pack.ExpectFullValueClassWithNoConstructorFromCommonBinary

fun consume(value: ExpectFullValueClassWithNoConstructorFromCommonBinary) {}
