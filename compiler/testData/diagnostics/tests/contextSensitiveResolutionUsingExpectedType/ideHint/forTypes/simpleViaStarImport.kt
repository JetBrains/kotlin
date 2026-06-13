// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

sealed class A {
    class X : A()
    class Y : A()
}

// FILE: main.kt
import test.A
import test.A.*

fun foo(a: A) {
    if (a is <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>X<!>) {
        "".hashCode()
    }

    if (a !is <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>) {
        "".hashCode()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, nestedClass, sealed,
stringLiteral */
