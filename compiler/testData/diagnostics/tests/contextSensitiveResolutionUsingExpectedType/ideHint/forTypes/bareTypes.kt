// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

sealed class A<T> {
    class X<T> : A<T>()
    class Y<T> : A<T>()
}

// FILE: main.kt
import test.A

fun foo(a: A<String>) {
    // Bare types — type arguments omitted
    if (a is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>) {
        "".hashCode()
    }

    when (a) {
        is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!> -> {}
        is <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!> -> {}
    }

    val x = a as <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
nestedClass, nullableType, propertyDeclaration, sealed, smartcast, stringLiteral, typeParameter, whenExpression,
whenWithSubject */
