// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

@Repeatable
annotation class Anno(val arr: IntArray)

@Anno([<!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>,])
@Anno([<!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>,<!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>,])
@Anno([<!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>,2])
@Anno([1, <!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>,])
@Anno([1, <!EMPTY_ARGUMENT_IN_COLLECTION_LITERAL!><!>, 3])
fun target() { }

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, vararg */
