// In this test, stdlib class `public value class Result<out T>` within `kotlin` package is replaced with a custom non-generic class

// NATIVE exception: java.util.NoSuchElementException: Collection contains no element matching the predicate
// during `KonanSymbols.kotlinResultGetOrThrow`, since function `Result<T>.getOrThrow()` within `kotlin` package cannot be found during builtins construction,
// since unbound class `Result` is expected, but deserialized one found.
// DONT_TARGET_EXACT_BACKEND: NATIVE

// WASM exception:
// There are still 4 unbound symbols after generation of IR module <main>:
// Unbound public symbol IrClassPublicSymbolImpl: kotlin/Result.Companion|null[0]
// Unbound public symbol IrClassPublicSymbolImpl: kotlin/Result.Failure|null[0]
// Unbound public symbol IrSimpleFunctionPublicSymbolImpl: kotlin/Result.Companion.success|8035950532576827725[0]
// Unbound public symbol IrSimpleFunctionPublicSymbolImpl: kotlin/Result.Failure.exception.<get-exception>|-7777496992132965566[0]
// This could happen if there are two libraries, where one library was compiled against the different version of the other library than the one currently used in the project. Please check that the project configuration is correct and has consistent versions of dependencies.
// IGNORE_BACKEND: WASM
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
