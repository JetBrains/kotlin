fun f(): Int {
    var i = 17
    <!UNUSED_FUNCTION_LITERAL!>{ (): Unit -> var <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 18 }<!>
    return i
}