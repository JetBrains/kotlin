// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE
// ISSUE: KT-85341

// FILE: test.kt
package test

enum class A {
    X, Y;

    companion object {
        val Z = X
    }
}

// FILE: main.kt
import test.A

fun expectsA(x: A) {}

fun main() {
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>)
    val a1: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>

    expectsA(A.Z)
    val a2: A = A.Z
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
