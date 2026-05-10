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

// A different class named X visible in scope — simple name X would resolve here, not to A.X
class X

fun foo(a: A) {
    // No hint: simple name X resolves to the local class X, not A.X
    if (a is A.X) {
        "".hashCode()
    }

    when (a) {
        is A.X -> {}
        is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!> -> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, nestedClass, sealed, smartcast,
stringLiteral, whenExpression, whenWithSubject */
