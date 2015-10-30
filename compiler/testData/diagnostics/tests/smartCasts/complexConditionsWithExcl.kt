fun foo(x: String?, y: String?, z: String?) {
    if ((x!!.hashCode() == 0 || y!!.hashCode() == 1) && z!!.hashCode() == 2) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        y<!UNSAFE_CALL!>.<!>length
        // condition is true => z!! after and is called
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    // First element is always analyzed
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    var xx = y ?: z
    if ((xx!!.hashCode() == 0 && y!!.hashCode() == 1) || z!!.hashCode() == 2) {
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        y<!UNSAFE_CALL!>.<!>length
        // condition is false => z!! after or is called
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
    // First element is always analyzed
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    xx = y ?: z
    if (xx!!.hashCode() == 0 && y!!.hashCode() == 1 && z!!.hashCode() == 2) {
        // all three are called
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    // First element is always analyzed
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    xx = y ?: z
    if (xx!!.hashCode() == 0 || y!!.hashCode() == 1 || z!!.hashCode() == 2) {
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    else {
        // all three are called
        <!DEBUG_INFO_SMARTCAST!>xx<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
}