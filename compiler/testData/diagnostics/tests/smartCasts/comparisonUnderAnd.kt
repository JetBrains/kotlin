fun foo(x : String?, y : String?) {
    if (y != null && x == y) {
        // Both not null
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    }
    else {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    if (y != null || x == <!ALWAYS_NULL!>y<!>) {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    else {
        // y == null but x != y
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!ALWAYS_NULL!>y<!><!UNSAFE_CALL!>.<!>length
    }
    if (y == null && x != <!ALWAYS_NULL!>y<!>) {
        // y == null but x != y
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!ALWAYS_NULL!>y<!><!UNSAFE_CALL!>.<!>length
    }
    else {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    if (y == null || x != y) {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    else {
        // Both not null
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    }
}