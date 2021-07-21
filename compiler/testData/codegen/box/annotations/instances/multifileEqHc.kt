// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_RUNTIME
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