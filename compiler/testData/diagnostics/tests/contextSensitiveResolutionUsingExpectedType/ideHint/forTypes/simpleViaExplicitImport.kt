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
import test.A.X
import test.A.Y

fun foo(a: A) {
    if (a is <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>X<!>) {
        "".hashCode()
    }

    if (a !is <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>) {
        "".hashCode()
    }

    val x = a as <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>X<!>
    val y = a <!CAST_NEVER_SUCCEEDS!>as?<!> <!DEBUG_INFO_CSR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>Y<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
nestedClass, nullableType, propertyDeclaration, sealed, smartcast, stringLiteral */
