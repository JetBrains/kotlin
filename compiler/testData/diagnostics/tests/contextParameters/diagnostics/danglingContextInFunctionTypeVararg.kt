// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno

fun withVararg(vararg p: context(Int)<!SYNTAX!><!>) {}

fun withVarargUnresolved(vararg p: context(Unresolved)<!SYNTAX!><!>) {}
fun withVarargType(vararg p: context(Int) String<!SYNTAX!><!>) {}
fun withVarargUnresolvedType(vararg p: context(Int) Unresolved<!SYNTAX!><!>) {}
fun withAnnotatedVararg(vararg p: @Anno context(Unresolved)<!SYNTAX!><!>) {}
fun withAnnotatedVarargType(vararg p: @Anno context(@Anno Int) Unresolved<!SYNTAX!><!>) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, vararg */
