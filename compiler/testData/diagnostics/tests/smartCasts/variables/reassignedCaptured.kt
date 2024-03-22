// SKIP_TXT
// FIR_IDENTICAL

fun callLambdaWithoutContract(block: () -> Unit) = block()

fun testIs(x: Any?) {
    var a = x
    val b = a
    if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
    if (b is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
    val f = {
        if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!> // bad
        if (b is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
    }
    a = ""
    f()
}

fun testInvertedIs(x: Any?) {
    var a = x
    val b = a
    if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
    if (b is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
    callLambdaWithoutContract { a = "" }
    if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!> // bad
    if (b is String) x.<!UNRESOLVED_REFERENCE!>length<!> // OK
}

fun testNotNull(x: String?) {
    var a = x
    val b = a
    if (a != null) x<!UNSAFE_CALL!>.<!>length // OK
    if (b != null) x<!UNSAFE_CALL!>.<!>length // OK
    val f = {
        if (a != null) x<!UNSAFE_CALL!>.<!>length // bad
        if (b != null) x<!UNSAFE_CALL!>.<!>length // OK
    }
    a = ""
    f()
}

fun testInvertedNotNull(x: String?) {
    var a = x
    val b = a
    if (a != null) x<!UNSAFE_CALL!>.<!>length // OK
    if (b != null) x<!UNSAFE_CALL!>.<!>length // OK
    callLambdaWithoutContract { a = "" }
    if (a != null) x<!UNSAFE_CALL!>.<!>length // bad
    if (b != null) x<!UNSAFE_CALL!>.<!>length // OK
}

fun testNotNullViaVariable(x: String?) {
    val a = x != null
    var b = a
    val c = b
    if (b) x<!UNSAFE_CALL!>.<!>length // OK
    if (c) x<!UNSAFE_CALL!>.<!>length // OK
    val f = {
        if (b) x<!UNSAFE_CALL!>.<!>length // bad
        if (c) x<!UNSAFE_CALL!>.<!>length // OK
    }
    b = true
    f()
}

fun testInvertedNotNullViaVariable(x: String?) {
    val a = x != null
    var b = a
    val c = b
    if (b) x<!UNSAFE_CALL!>.<!>length // OK
    if (c) x<!UNSAFE_CALL!>.<!>length // OK
    callLambdaWithoutContract { b = true }
    if (b) x<!UNSAFE_CALL!>.<!>length // bad
    if (c) x<!UNSAFE_CALL!>.<!>length // OK
}
