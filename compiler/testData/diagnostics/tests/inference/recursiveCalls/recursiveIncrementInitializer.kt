// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-88576

object Foo {
    val x1 = ++<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x1<!>
    val x2 = <!RECURSION_IN_IMPLICIT_TYPES!><!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x2<!>++<!>
}

/* GENERATED_FIR_TAGS: assignment, incrementDecrementExpression, objectDeclaration, propertyDeclaration */
