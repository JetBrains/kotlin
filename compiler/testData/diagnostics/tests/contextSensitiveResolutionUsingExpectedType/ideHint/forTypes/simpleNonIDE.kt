// RUN_PIPELINE_TILL: BACKEND

// FILE: test.kt
package test

sealed class A {
    class X : A()
    class Y : A()
}

// FILE: main.kt
import test.A

fun foo(a: A) {
    if (a is A.X) {
        "".hashCode()
    }

    when (a) {
        is A.X -> {}
        is A.Y -> {}
    }

    val x = a as A.X
    val y = a <!CAST_NEVER_SUCCEEDS!>as?<!> A.Y
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
nestedClass, nullableType, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
