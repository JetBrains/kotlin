fun unusedLiteral(){
    <!UNUSED_FUNCTION_LITERAL!>{() ->
        val <!UNUSED_VARIABLE!>i<!> = 1
    }<!>
}


fun unusedLiteralInDoWhile(){
    do<!UNUSED_FUNCTION_LITERAL!>{() ->
            val <!UNUSED_VARIABLE!>i<!> = 1
    }<!> while(false)
}