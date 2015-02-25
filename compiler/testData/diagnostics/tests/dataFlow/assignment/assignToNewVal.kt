fun test(a: Any?) {
    if (a == null) return
    <!DEBUG_INFO_SMARTCAST!>a<!>.hashCode()

    val b = a
    <!DEBUG_INFO_SMARTCAST!>b<!>.hashCode()

    val c: Any? = a
    c<!UNSAFE_CALL!>.<!>hashCode()
}