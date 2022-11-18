fun test(a: Any?, flag: Boolean, x: Any?) {
    if (a !is String) return
    a.length

    val b: Any?

    if (flag) {
        b = a
        b.length
    }
    else {
        b = x
        <!ARGUMENT_TYPE_MISMATCH!>b.<!UNRESOLVED_REFERENCE!>length<!>()<!>
    }
}
