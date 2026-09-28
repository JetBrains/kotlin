// RUN_PIPELINE_TILL: CODEGEN
// IDE_MODE
// ISSUE: KT-80943

// FILE: test.kt
package test

enum class A {
    X
}

// FILE: main.kt
import test.A

class X

fun expectsA(x: A) {}

fun main() {
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>)
    val a: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, localProperty,
propertyDeclaration */
