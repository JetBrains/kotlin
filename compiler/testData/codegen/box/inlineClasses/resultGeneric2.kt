// NATIVE, WASM, JS_IR errors are same as for `resultGeneric.kt`
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
value class Result<T: Any>(val value: T?)

// FILE: box.kt

fun box(): String {
    return Result("OK").value!!
}
