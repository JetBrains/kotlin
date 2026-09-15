// LANGUAGE: +FullValueClasses

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

sealed value class Result

value class Success(val value: String, val code: Int) : Result()
value object Empty : Result()
class Failure(val message: String) : Result()

// MODULE: main(lib)
// FILE: main.kt

// class: test/Result
