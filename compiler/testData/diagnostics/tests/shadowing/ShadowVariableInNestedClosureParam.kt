fun ff(): Int {
    var i = 1
    <!UNUSED_LAMBDA_EXPRESSION!>{ i: Int -> i }<!>
    return i
}
