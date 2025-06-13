// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
fun test() {
    <!WRONG_ANNOTATION_TARGET!>@ann<!>
    while (2 > 1) {}

    <!WRONG_ANNOTATION_TARGET!>@ann<!>
    do {} while (2 > 1)

    <!WRONG_ANNOTATION_TARGET!>@ann<!>
    for (i in 1..2) {}
}

annotation class ann

/* GENERATED_FIR_TAGS: annotationDeclaration, comparisonExpression, doWhileLoop, forLoop, functionDeclaration,
integerLiteral, localProperty, propertyDeclaration, rangeExpression, whileLoop */
