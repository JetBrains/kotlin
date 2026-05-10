// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

enum class A {
    X, Y
}

// FILE: main.kt
import test.A

fun foo(a: A) {
    if (a == <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>) {
        "".hashCode()
    }

    when (a) {
        <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!> -> {}
        <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!> -> {}
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, smartcast,
stringLiteral, whenExpression, whenWithSubject */
