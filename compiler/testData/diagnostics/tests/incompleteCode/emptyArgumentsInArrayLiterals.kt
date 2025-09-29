// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

@Repeatable
annotation class Anno(val arr: IntArray)

@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ARGUMENT_EXPECTED!><!>,]<!>)
@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ARGUMENT_EXPECTED!><!>,<!ARGUMENT_EXPECTED!><!>,]<!>)
@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ARGUMENT_EXPECTED!><!>,2]<!>)
@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[1, <!ARGUMENT_EXPECTED!><!>,]<!>)
@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[1, <!ARGUMENT_EXPECTED!><!>, 3]<!>)
fun target() { }

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, vararg */
