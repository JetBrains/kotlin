class P

fun foo(p: P): Any {
    val v = p <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> <!UNRESOLVED_REFERENCE!>G<!>
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v<!>
}