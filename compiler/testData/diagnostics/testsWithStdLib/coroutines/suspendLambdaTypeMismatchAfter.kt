// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ParseLambdaWithSuspendModifier

fun foo(f: () -> Unit) {}

fun test() {
    foo(suspend <!ARGUMENT_TYPE_MISMATCH!>{}<!>)
    val x: () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> suspend {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration */
