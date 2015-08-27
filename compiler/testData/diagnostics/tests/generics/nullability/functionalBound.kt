fun <E : String?, T : ((CharSequence).() -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!UNSAFE_CALL!>y<!>()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!><!DEBUG_INFO_SMARTCAST!>y<!>()
    }

    if (x != null && y != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!DEBUG_INFO_SMARTCAST!>y<!>()
    }
}
