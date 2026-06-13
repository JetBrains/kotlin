// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

class A

object Source {
    val X: A = A()
}

// FILE: main.kt
import test.A
import test.Source.X

fun expectsA(x: A) {}

fun main() {
    // No hint expected: `X` is resolved through ExplicitImport to `Source.X`,
    // but `A` has no static / companion / sealed-nested `X` for CSR to find,
    // so CSR cannot reach the same symbol via the expected type.
    expectsA(X)
    val a: A = X
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, objectDeclaration, propertyDeclaration */
