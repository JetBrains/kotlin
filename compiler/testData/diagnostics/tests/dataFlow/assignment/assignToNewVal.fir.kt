fun test(a: Any?) {
    if (a == null) return
    a.hashCode()

    val b = a
    b.hashCode()

    val c: Any? = a
    c<!UNSAFE_CALL!>.<!>hashCode()
}
