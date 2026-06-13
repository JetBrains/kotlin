// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// DIAGNOSTICS: -REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION -REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION

enum class E1 {
    X1
}

enum class E2 {
    X2
}

@Repeatable
annotation class A1(vararg val e1: E1, val e2: Array<E2>)
@Repeatable
annotation class A2(val e1: Array<E1>, vararg val e2: E2)
@Repeatable
annotation class A3(val e1: Array<E1>, val e2: Array<E2>)

@A1(*[], e2 = [])
@A1(*[X1, X1, X1], e2 = [])
@A1(*[X1], *[], *[X1], e2 = arrayOf(X2))
@A1(e1 = [X1], e2 = [X2])
@A1(e1 = *[X1], e2 = [X2])
@A1(e2 = [X2, X2, X2], e1 = [X1, X1])
@A1(e2 = [], e1 = *[])
@A1(e2 = [])
@A1(*arrayOf(*arrayOf(*<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X1<!>, <!UNRESOLVED_REFERENCE!>X1<!>]<!>, X1, X1, *arrayOf(X1, *<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X1<!>]<!>)), *[X1, X1, X1]), X1, *[X1, X1, X1], e2 = arrayOf(*[X2, X2], X2, *arrayOf(X2, *<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X2<!>, <!UNRESOLVED_REFERENCE!>X2<!>]<!>)))
@A1(e2 = arrayOf(*[X2, X2], *[X2], *[]))
fun test1() = Unit

@A2([], X2)
@A2([X1], X2)
@A2([X1])
@A2([X1, X1, X1], X2, X2, X2)
@A2(arrayOf(*[X1], X1, *arrayOf(*<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X1<!>]<!>, *<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X1<!>]<!>, *<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X1<!>]<!>)))
@A2(e1 = [], *arrayOf(X2, X2, X2, *[X2, X2, X2], X2, X2, X2, *arrayOf(*<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X2<!>]<!>)))
@A2(e1 = [], e2 = [])
@A2(e1 = [], e2 = *[X2, X2, X2])
@A2(e2 = [X2], e1 = [X1])
// TODO (KT-86481): this and some other annotation calls currently crash backend
@A2(e2 = *[X2], e1 = arrayOf(*[X1]))
fun test2() = Unit

@A3([], [])
@A3(e1 = arrayOf(*[X1, X1], X1, *arrayOf()), e2 = [])
@A3(e2 = arrayOf(*arrayOf<E2>(*<!ARGUMENT_TYPE_MISMATCH!>[<!UNRESOLVED_REFERENCE!>X2<!>, <!UNRESOLVED_REFERENCE!>X2<!>]<!>)), e1 = [])
fun test3() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, functionDeclaration,
outProjection, primaryConstructor, propertyDeclaration, vararg */
