// RUN_PIPELINE_TILL: CODEGEN
// AssertionError for nested ifs with lambdas and Nothing as results

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
