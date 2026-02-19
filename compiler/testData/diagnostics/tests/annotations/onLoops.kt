// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
fun test() {
    @ann
    while (2 < 1) {}

    @ann
    do {} while (2 < 1)

    @ann
    for (i in 1..2) {}
}

annotation class ann

/* GENERATED_FIR_TAGS: annotationDeclaration, comparisonExpression, doWhileLoop, forLoop, functionDeclaration,
integerLiteral, localProperty, propertyDeclaration, rangeExpression, whileLoop */
