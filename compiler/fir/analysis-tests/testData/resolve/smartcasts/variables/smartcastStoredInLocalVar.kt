// WITH_STDLIB
// ISSUE: KT-58604

fun nonInPlaceRun(block: () -> Unit) {}

fun test_0(a: Any) {
    var b = a is String
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // can be ok
    }
    nonInPlaceRun {
        if (b) {
            a.<!UNRESOLVED_REFERENCE!>length<!> // can be ok
        }
    }
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // can be ok
    }
}

fun test_1(a: Any) {
    var b = a is String
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // can be ok
    }
    nonInPlaceRun {
        if (b) {
            a.<!UNRESOLVED_REFERENCE!>length<!> // not ok
        }
    }
    b = true
}

fun test_2(a: Any) {
    var b = a is String
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // can be ok
    }
    nonInPlaceRun {
        b = true
    }
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // not ok
    }
}

fun test_3(a: Any) {
    var b = a is String
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // // can be ok
    }
    run {
        if (b) {
            a.<!UNRESOLVED_REFERENCE!>length<!> // // can be ok
        }
    }
    b = true
}

fun test_4(a: Any) {
    var b = a is String
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // // can be ok
    }
    run {
        b = true
    }
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // not ok
    }
}

fun test_5(a: Any) {
    var b = a is String
    while (b) {
        if (b) {
            a.<!UNRESOLVED_REFERENCE!>length<!> // not ok
        }
        b = a is String
    }
    if (b) {
        a.<!UNRESOLVED_REFERENCE!>length<!> // // can be ok
    }
}
