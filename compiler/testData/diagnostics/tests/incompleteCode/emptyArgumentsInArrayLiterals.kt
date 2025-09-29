// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

@Repeatable
annotation class Anno(vararg val arr: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>IntArray<!>)

@Anno([<!SYNTAX!>,<!>])
@Anno([<!SYNTAX!>,<!><!SYNTAX!><!>,])
@Anno([<!SYNTAX!>,<!>2])
@Anno([1, <!SYNTAX!>,<!>])
@Anno([1, <!SYNTAX!>,<!> 3])
fun target() { }

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, vararg */
