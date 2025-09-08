// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION, +UNUSED_VARIABLE

fun unusedLiteral(){
    { ->
        val i = 1
    }
}


fun unusedLiteralInDoWhile(){
    do{ ->
            val i = 1
    } while(false)
}

/* GENERATED_FIR_TAGS: doWhileLoop, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
