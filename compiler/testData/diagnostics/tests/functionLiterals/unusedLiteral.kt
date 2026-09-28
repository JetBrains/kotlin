// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION, +UNUSED_VARIABLE

fun unusedLiteral(){
    <!UNUSED_LAMBDA_EXPRESSION!>{ ->
        val i = 1
    }<!>
}


fun unusedLiteralInDoWhile(){
    do<!UNUSED_LAMBDA_EXPRESSION!>{ ->
            val i = 1
    }<!> while(false)
}

/* GENERATED_FIR_TAGS: doWhileLoop, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
