// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ParseLambdaWithSuspendModifier

fun foo(f: () -> Unit) {}

fun test() {
    foo(<!TYPE_MISMATCH!>suspend {}<!>)
    val x: () -> Unit = <!TYPE_MISMATCH, TYPE_MISMATCH!>suspend {}<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration */
