// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS

// (supported: JVM_IR, JS_IR(_E6))

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses


// FILE: a.kt
package a

annotation class A(val i: Int)

fun createInOtherFile(): A = A(10)

// FILE: b.kt

import a.*

fun here(): A = A(10)

fun box(): String {
    if (here() != createInOtherFile()) return "Fail equals"
    if (here().hashCode() != createInOtherFile().hashCode()) return "Fail hashCode"
    if (here().toString() != createInOtherFile().toString()) return "Fail toString"
    return "OK"
}
