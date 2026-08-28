// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-88576

object Foo {
    val x1 = ++x1
    val x2 = <!RECURSION_IN_IMPLICIT_TYPES!>x2++<!>
}

/* GENERATED_FIR_TAGS: assignment, incrementDecrementExpression, objectDeclaration, propertyDeclaration */
