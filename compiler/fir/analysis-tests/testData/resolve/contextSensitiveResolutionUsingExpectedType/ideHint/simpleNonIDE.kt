// RUN_PIPELINE_TILL: BACKEND

// FILE: test.kt
package test

enum class A {
    X, Y
}

// FILE: main.kt
import test.A

fun expectsA(x: A) {}

fun main() {
    expectsA(A.X)
    val a: A = A.Y
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
