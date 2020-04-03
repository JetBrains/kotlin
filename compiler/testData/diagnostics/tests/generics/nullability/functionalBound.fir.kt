fun <E : String?, T : ((CharSequence) -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        y(x)
    }

    if (y != null) {
        <!INAPPLICABLE_CANDIDATE!>y<!>(x)
    }

    if (x != null && y != null) {
        y(x)
    }
}