// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-56186

class Foo<I, J : Number, K> {
    val value: String = "OK"
    val genericValue: Triple<I, J, K> = TODO()
}

fun test_1() {
    val a = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<!>::<!UNRESOLVED_REFERENCE!>value<!>
    val b = Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::<!UNRESOLVED_REFERENCE!>value<!>
    val c = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<!>::<!UNRESOLVED_REFERENCE!>genericValue<!>
    val d = Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::<!UNRESOLVED_REFERENCE!>genericValue<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localProperty, nullableType,
propertyDeclaration, starProjection, stringLiteral, typeConstraint, typeParameter */
