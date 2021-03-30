fun test(a: Any?, flag: Boolean, x: Any?) {
    if (a == null) return
    a.hashCode()

    val b: Any?

    if (flag) {
        b = a
        b.hashCode()
    }
    else {
        b = x
        b<!UNSAFE_CALL!>.<!>hashCode()
    }
}