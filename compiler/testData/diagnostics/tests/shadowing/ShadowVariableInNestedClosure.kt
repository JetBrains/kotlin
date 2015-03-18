fun f(): Int {
    var i = 17
    <!UNUSED_FUNCTION_LITERAL!>{ var <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 18 }<!>
    return i
}