// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun f(): Int {
    var i = 17
    <!UNUSED_LAMBDA_EXPRESSION!>{ var i = 18 }<!>
    return i
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration */
