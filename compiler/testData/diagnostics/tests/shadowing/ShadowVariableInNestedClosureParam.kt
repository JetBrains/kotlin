fun ff(): Int {
    var i = 1
    <!UNUSED_LAMBDA_EXPRESSION!>{ <!NAME_SHADOWING!>i<!>: Int -> i }<!>
    return i
}
