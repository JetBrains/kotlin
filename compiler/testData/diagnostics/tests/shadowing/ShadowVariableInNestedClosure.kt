fun f(): Int {
    var i = 17
    <!UNUSED_LAMBDA_EXPRESSION!>{ var <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 18 }<!>
    return i
}