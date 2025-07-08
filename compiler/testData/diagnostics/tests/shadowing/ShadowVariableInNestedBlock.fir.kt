// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun ff(): Int {
    var i = 1
    <!UNUSED_LAMBDA_EXPRESSION!>{
        val i = 2
    }<!>
    return i
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration */
