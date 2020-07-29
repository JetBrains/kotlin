interface Inv<T>

fun <Y: X, X : Inv<out String>> foo(x: X, y: Y) {
    val rX = <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    rX.length

    val rY = <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
    rY.length
}

fun <Y> bar(l: Inv<Y>): Y = TODO()
