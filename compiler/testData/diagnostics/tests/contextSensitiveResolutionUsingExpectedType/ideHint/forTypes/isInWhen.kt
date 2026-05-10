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

fun foo(a: A) {
    when (a) {
        is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!> -> {}
        is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!> -> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, nestedClass, sealed, smartcast,
whenExpression, whenWithSubject */
