class P

fun foo(p: P): Any {
    val v = p as <!UNRESOLVED_REFERENCE!>G<!>
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v<!>
}

fun bar(p: P): Any {
    val v = p as <!UNRESOLVED_REFERENCE!>G<!>?
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v<!>
}
