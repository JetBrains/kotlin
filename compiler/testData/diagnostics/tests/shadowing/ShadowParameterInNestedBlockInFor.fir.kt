// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_PARAMETER +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun f(i: Int) {
    for (j in 1..100) {
        <!UNUSED_LAMBDA_EXPRESSION!>{
            var i = 12
        }<!>
    }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
rangeExpression */
