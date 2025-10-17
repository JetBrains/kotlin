// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80606
// FIR_IDENTICAL

open class A(val s: Any)

val foo: Any
    field = object : A(<!UNINITIALIZED_VARIABLE!>foo<!>) {}

val bar: Any
    field = object : A(<!UNINITIALIZED_VARIABLE!>baz<!>) {}

val baz: Any
    field = object : A(bar) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, explicitBackingField, primaryConstructor,
propertyDeclaration, smartcast */
