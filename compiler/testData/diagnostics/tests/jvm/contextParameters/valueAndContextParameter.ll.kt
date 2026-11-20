// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

context(a: A)
fun foo(){}

fun foo(a: A){}

context(a: A, b: B)
fun bar(){}

context(a: A)
fun bar(b: B){}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
