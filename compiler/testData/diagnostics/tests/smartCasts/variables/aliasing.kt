// ISSUE: KT-56744
// SKIP_TXT

fun test() {
    var a: Any? = null
    var b = a
    var c = b
    // Now both `b` and `c` are aliases of `a`.

    if (a is String) {
        b.<!UNRESOLVED_REFERENCE!>length<!> // OK
        c.<!UNRESOLVED_REFERENCE!>length<!> // OK
    }
    if (b is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // OK
        c.<!UNRESOLVED_REFERENCE!>length<!> // OK
    }
    if (c is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // OK
        b.<!UNRESOLVED_REFERENCE!>length<!> // OK
    }

    b = 3 // break `b` -> `a`
    if (a is String) {
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // OK, since `c` is aliased to `a`
    }
    if (<!USELESS_IS_CHECK!>b is String<!>) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (c is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // OK, since `c` is alised to `a`
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
    }

    a = 2 // break `c` -> `a`
    if (<!USELESS_IS_CHECK!>a is String<!>) {
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (<!USELESS_IS_CHECK!>b is String<!>) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (c is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
    }

    c = b // create aliasing `c` -> `b`
    <!DEBUG_INFO_SMARTCAST!>c<!>.unaryPlus() // OK due to aliasing
    b = ""
    <!DEBUG_INFO_SMARTCAST!>c<!>.unaryPlus() // OK since `c` should carry all typing information that was on `b` before `b = ""`.

    c = ""
    <!DEBUG_INFO_SMARTCAST!>c<!>.length // OK
    c.<!UNRESOLVED_REFERENCE!>unaryPlus<!>() // error
}

fun test2() {
    var a: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
    a = ""
    var b = a
    a = 3
    <!DEBUG_INFO_SMARTCAST!>b<!>.length // OK
    b.<!UNRESOLVED_REFERENCE!>unaryPlus<!>() // error
}

fun test3() {
    var a: Any? = null
    val b = a
    val c = a
    if (a is String) {
        <!DEBUG_INFO_SMARTCAST!>a<!>.length // ok
        b.<!UNRESOLVED_REFERENCE!>length<!> // ok
        c.<!UNRESOLVED_REFERENCE!>length<!> // ok
    }
    a = null // b and c are still aliases to the same old value
    if (b is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        <!DEBUG_INFO_SMARTCAST!>b<!>.length // ok
        c.<!UNRESOLVED_REFERENCE!>length<!> // ok
    }
}
