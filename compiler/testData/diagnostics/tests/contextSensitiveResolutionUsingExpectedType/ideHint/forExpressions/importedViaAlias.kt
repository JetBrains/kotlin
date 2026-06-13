// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

sealed class A {
    object X : A()
    object Y : A()
}

// FILE: main.kt
import test.A
import test.A.X as XX
import test.A.Y

fun expectsA(x: A) {}

fun main() {
    val x: A = XX
    val y: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>

    expectsA(XX)
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nestedClass, objectDeclaration,
propertyDeclaration, sealed */
