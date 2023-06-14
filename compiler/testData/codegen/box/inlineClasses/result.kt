// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
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
