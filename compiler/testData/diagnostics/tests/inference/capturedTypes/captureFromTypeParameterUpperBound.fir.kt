interface Inv<T>

fun <Y: X, X : Inv<out String>> foo(x: X, y: Y) {
    val rX = bar(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    rX.<!UNRESOLVED_REFERENCE!>length<!>

    val rY = bar(<!ARGUMENT_TYPE_MISMATCH!>y<!>)
    rY.<!UNRESOLVED_REFERENCE!>length<!>
}

fun <Y> bar(l: Inv<Y>): Y = TODO()
