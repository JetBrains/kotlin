// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun test1() {
    try {

    } catch (<!SYNTAX!><!>)<!SYNTAX!><!>
}

fun test2() {
    try { }<!SYNTAX!><!>
}

fun test3() {
    try {
    } catch (<!SYNTAX!><!>{}<!SYNTAX!>)<!> {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration, tryExpression */
