// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno

fun withVararg(vararg p: context(Int)<!SYNTAX!><!>) {}

fun withVarargUnresolved(vararg p: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>) {}
fun withVarargType(vararg p: context(Int) String<!SYNTAX!><!>) {}
fun withVarargUnresolvedType(vararg p: context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>) {}
fun withAnnotatedVararg(vararg p: @Anno context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>) {}
fun withAnnotatedVarargType(vararg p: @Anno context(@Anno Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>) {}
fun withNullableVararg(vararg p: (@Anno context(<!UNRESOLVED_REFERENCE!>Unresolved<!>) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>)?) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, vararg */
