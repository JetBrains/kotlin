// RUN_PIPELINE_TILL: BACKEND

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
    if (a is X) {
        "".hashCode()
    }

    if (a !is Y) {
        "".hashCode()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, nestedClass, sealed,
stringLiteral */
