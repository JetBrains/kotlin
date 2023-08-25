class P

fun foo(p: P): Any {
    val v = p as <!UNRESOLVED_REFERENCE!>G<!>
    return v
}

fun bar(p: P): Any {
    val v = p as <!UNRESOLVED_REFERENCE!>G<!>?
    return v
}
