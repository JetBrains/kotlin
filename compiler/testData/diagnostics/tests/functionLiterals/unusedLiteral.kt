// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION, +UNUSED_VARIABLE

fun unusedLiteral(){
    <!UNUSED_LAMBDA_EXPRESSION!>{ ->
        val <!UNUSED_VARIABLE!>i<!> = 1
    }<!>
}


fun unusedLiteralInDoWhile(){
    do<!UNUSED_LAMBDA_EXPRESSION!>{ ->
            val <!UNUSED_VARIABLE!>i<!> = 1
    }<!> while(false)
}
