// RUN_PIPELINE_TILL: BACKEND
// AssertionError for nested ifs with lambdas and Nothing as results
// NI_EXPECTED_FILE

val fn = if (true) {
    { true }
}
else if (true) {
    { true }
}
else {
    null!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, ifExpression, lambdaLiteral, propertyDeclaration */
