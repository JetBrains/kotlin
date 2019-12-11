// !WITH_NEW_INFERENCE

fun f(c: LongRange): Int {
    return c.<!UNRESOLVED_REFERENCE!>start<!>()
}
