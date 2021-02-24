fun foo(x: String?, y: String?, z: String?) {
    if ((x!!.hashCode() == 0 || y!!.hashCode() == 1) && z!!.hashCode() == 2) {
        x.length
        y<!UNSAFE_CALL!>.<!>length
        // condition is true => z!! after and is called
        z.length
    }
    else {
        x.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    // First element is always analyzed
    x.length
    var xx = y ?: z
    if ((xx!!.hashCode() == 0 && y!!.hashCode() == 1) || z!!.hashCode() == 2) {
        xx.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    else {
        xx.length
        y<!UNSAFE_CALL!>.<!>length
        // condition is false => z!! after or is called
        z.length
    }
    // First element is always analyzed
    x.length
    xx = y ?: z
    if (xx!!.hashCode() == 0 && y!!.hashCode() == 1 && z!!.hashCode() == 2) {
        // all three are called
        xx.length
        y.length
        z.length
    }
    else {
        xx.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    // First element is always analyzed
    x.length
    xx = y ?: z
    if (xx!!.hashCode() == 0 || y!!.hashCode() == 1 || z!!.hashCode() == 2) {
        xx.length
        y<!UNSAFE_CALL!>.<!>length
        z<!UNSAFE_CALL!>.<!>length
    }
    else {
        // all three are called
        xx.length
        y.length
        z.length
    }
}