fun test(a: Any?, flag: Boolean, x: Any?) {
    if (a == null) return
    <!DEBUG_INFO_SMARTCAST!>a<!>.hashCode()

    val b: Any?

    if (flag) {
        b = a
        <!DEBUG_INFO_SMARTCAST!>b<!>.hashCode()
    }
    else {
        b = x
        b<!UNSAFE_CALL!>.<!>hashCode()
    }
}