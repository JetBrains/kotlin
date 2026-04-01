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

fun main() {
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>)
    val a: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
