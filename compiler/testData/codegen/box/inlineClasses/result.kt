// In this test, stdlib class `public value class Result<out T>` within `kotlin` package is replaced with a custom non-generic class

// NATIVE exception: java.util.NoSuchElementException: Collection contains no element matching the predicate
// during `KonanSymbols.kotlinResultGetOrThrow`, since function `Result<T>.getOrThrow()` within `kotlin` package cannot be found during builtins construction,
// since unbound class `Result` is expected, but deserialized one found.
// DONT_TARGET_EXACT_BACKEND: NATIVE
// IGNORE_BACKEND: ANDROID
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: result.kt
package kotlin

OPTIONAL_JVM_INLINE_ANNOTATION
value class Result(val value: Any?)

// FILE: box.kt

fun box(): String {
    return Result("OK").value as String
}
