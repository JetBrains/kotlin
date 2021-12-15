fun foo(x : String?, y : String?) {
    if (y != null && x == y) {
        // Both not null
        x.length
        y.length
    }
    else {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    if (y != null || x == y) {
        x<!UNSAFE_CALL!>.<!>length
        y<!UNSAFE_CALL!>.<!>length
    }
    else {
        // y == null but x != y
        x.length
        y<!UNSAFE_CALL!>.<!>length
    }
    if (y == null && x != y) {
        // y == null but x != y
        x.length
        y<!UNSAFE_CALL!>.<!>length
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
        x.length
        y.length
    }
}
