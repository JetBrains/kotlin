// SET_FALSE: CALL_PARAMETERS_LPAREN_ON_NEXT_LINE
// SET_FALSE: CALL_PARAMETERS_RPAREN_ON_NEXT_LINE
fun f() {
    foo(<caret>1, "a", 2)
}

fun foo(p1: Int, p2: String, p3: Int){}