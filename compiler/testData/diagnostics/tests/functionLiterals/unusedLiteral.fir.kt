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