// NATIVE and WASM failure reasons see in `result.kt`
// JS_IR error: <main> @ /box.kt:24:12: Constructor 'Result.<init>' can not be called: No constructor found for symbol 'kotlin/Result.<init>|-8731461708390519279[0]'
// DONT_TARGET_EXACT_BACKEND: NATIVE
// IGNORE_BACKEND: WASM, JS_IR, JS_IR_ES6
// IGNORE_BACKEND: ANDROID

// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: result.kt
package kotlin

OPTIONAL_JVM_INLINE_ANNOTATION
value class Result<T>(val value: T)

// FILE: box.kt

fun box(): String {
    return Result("OK").value
}
