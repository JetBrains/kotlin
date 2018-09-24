// See KT-6665: unresolved reference (v.bar) should not produce "unreachable code" after it

fun foo(): Int {
    val v = 1
    val <!UNUSED_VARIABLE!>c<!> = v.<!UNRESOLVED_REFERENCE!>bar<!> ?: return 0
    return 42
}

fun foo2(): Int {
    val v = 1
    val c = if (true) v.<!UNRESOLVED_REFERENCE!>bar<!> else return 3
    val <!UNUSED_VARIABLE!>b<!> = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>c<!>
    return 42
}

fun foo3(): Int {
    val v = 1
    val c = when {
        true -> v.<!UNRESOLVED_REFERENCE!>bar<!>
        else -> return 3
    }
    val <!UNUSED_VARIABLE!>b<!> = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>c<!>
    return 42
}

// Type + ErrorType should give Type, unless Type is Nothing

fun bar(): Int {
    val v = 1
    val c = v.<!UNRESOLVED_REFERENCE!>bar<!> ?: 42
    return c
}

fun bar2(): Int {
    val v = 1
    val c = if (true) v.<!UNRESOLVED_REFERENCE!>bar<!> else 3
    val b = c
    return b
}

fun bar3(): Int {
    val v = 1
    val c = when {
        true -> v.<!UNRESOLVED_REFERENCE!>bar<!>
        else -> 3
    }
    val b = c
    return b
}