fun ff(): Int {
    var i = 1
    <!UNUSED_FUNCTION_LITERAL!>{ (i: Int) -> i }<!>
    return i
}
