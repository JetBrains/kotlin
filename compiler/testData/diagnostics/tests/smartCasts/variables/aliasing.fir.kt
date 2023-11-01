// ISSUE: KT-56744
// SKIP_TXT

fun test() {
    var a: Any? = null
    var b = a
    var c = b
    // Now both `b` and `c` are aliases of `a`.

    if (a is String) {
        b.length // OK
        c.length // OK
    }
    if (b is String) {
        a.length // OK
        c.length // OK
    }
    if (c is String) {
        a.length // OK
        b.length // OK
    }

    b = 3 // break `b` -> `a`
    if (a is String) {
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.length // OK, since `c` is aliased to `a`
    }
    if (b is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (c is String) {
        a.length // OK, since `c` is alised to `a`
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
    }

    a = 2 // break `c` -> `a`
    if (a is String) {
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (b is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        c.<!UNRESOLVED_REFERENCE!>length<!> // error
    }
    if (c is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        b.<!UNRESOLVED_REFERENCE!>length<!> // error
    }

    c = b // create aliasing `c` -> `b`
    c.unaryPlus() // OK due to aliasing
    b = ""
    c.unaryPlus() // OK since `c` should carry all typing information that was on `b` before `b = ""`.

    c = ""
    c.length // OK
    c.<!UNRESOLVED_REFERENCE!>unaryPlus<!>() // error
}

fun test2() {
    var a: Any? = null
    a = ""
    var b = a
    a = 3
    b.length // OK
    b.<!UNRESOLVED_REFERENCE!>unaryPlus<!>() // error
}

fun test3() {
    var a: Any? = null
    val b = a
    val c = a
    if (a is String) {
        a.length // ok
        b.length // ok
        c.length // ok
    }
    a = null // b and c are still aliases to the same old value
    if (b is String) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // error
        b.length // ok
        c.length // ok
    }
}
