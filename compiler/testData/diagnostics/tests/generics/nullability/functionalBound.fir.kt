fun <E : String?, T : ((CharSequence) -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        <!INAPPLICABLE_CANDIDATE!>y<!>(x)
    }

    if (y != null) {
        y(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    }

    if (x != null && y != null) {
        y(x)
    }
}
