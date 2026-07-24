// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

enum class A {
    X, Y
}

// FILE: main.kt
import test.A
import test.A.X
import test.A.Y

fun expectsA(x: A) {}

fun main() {
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>X<!>)
    val a: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
