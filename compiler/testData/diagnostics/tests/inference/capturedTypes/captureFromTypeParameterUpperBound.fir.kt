interface Inv<T>

fun <Y: X, X : Inv<out String>> foo(x: X, y: Y) {
    val rX = <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    rX.<!UNRESOLVED_REFERENCE!>length<!>

    val rY = <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
    rY.<!UNRESOLVED_REFERENCE!>length<!>
}

fun <Y> bar(l: Inv<Y>): Y = TODO()