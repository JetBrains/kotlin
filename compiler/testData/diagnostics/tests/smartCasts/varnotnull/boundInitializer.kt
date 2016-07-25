fun foo(arg: Int?) {
    val x = arg
    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>arg<!>.hashCode()
    }
    val y: Any? = arg
    if (y != null) {
        <!DEBUG_INFO_SMARTCAST!>arg<!>.hashCode()
    }
    val yy: Any?
    yy = arg
    if (yy != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
    var z = arg
    z = z?.let { 42 }
    if (z != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
}
