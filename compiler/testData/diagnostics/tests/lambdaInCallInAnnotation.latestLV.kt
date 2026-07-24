// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

annotation class Anno(val x: String)

@Anno(x = <!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>({ <!UNRESOLVED_REFERENCE!>it<!> })<!>)
fun method() {
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
