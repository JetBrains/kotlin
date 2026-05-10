// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

enum class A {
    X, Y
}

// FILE: main.kt
import test.A

fun expectsA(x: A) {}

val X: A = A.X

fun main() {
    expectsA(A.X)
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>)
    val a1: A = A.X
    val a2: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
