// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ParseLambdaWithSuspendModifier

fun foo(f: () -> Unit) {}

fun test() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>suspend {}<!>)
    val x: () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> suspend {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration */
