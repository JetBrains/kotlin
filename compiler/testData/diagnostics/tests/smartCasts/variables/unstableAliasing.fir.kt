// WITH_STDLIB

fun callLambdaWithoutContract(block: () -> Unit) = block()

fun testIs(x: Any?) {
    var a = x
    val b = a
    if (a is String) x.length
    if (b is String) x.length
    val f = {
        if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!>
        if (b is String) x.length
    }
    a = ""
    f()
}

fun testIsLambda(x: Any?) {
    var a = x
    val b = a
    if (a is String) x.length
    if (b is String) x.length
    callLambdaWithoutContract { a = "" }
    if (a is String) x.<!UNRESOLVED_REFERENCE!>length<!>
    if (b is String) x.length
}

fun testRequireIs(x: Any?) {
    var a = x
    val b = a
    val f: () -> Unit = {
        require(a is String)
        x.<!UNRESOLVED_REFERENCE!>length<!>
        require(b is String)
        x.length
    }
    a = ""
    f()
}

fun testRequireIsLambda(x: Any?) {
    var a = x
    val b = a
    callLambdaWithoutContract { a = "" }
    require(a is String)
    x.<!UNRESOLVED_REFERENCE!>length<!>
    require(b is String)
    x.length
}

fun testNotNull(x: String?) {
    var a = x
    val b = a
    if (a != null) x.length
    if (b != null) x.length
    val f = {
        if (a != null) x<!UNSAFE_CALL!>.<!>length
        if (b != null) x.length
    }
    a = ""
    f()
}

fun testNotNullLambda(x: String?) {
    var a = x
    val b = a
    if (a != null) x.length
    if (b != null) x.length
    callLambdaWithoutContract { a = "" }
    if (a != null) x<!UNSAFE_CALL!>.<!>length
    if (b != null) x.length
}

fun testRequireNotNull(x: String?) {
    var a = x
    val b = a
    val f: () -> Unit = {
        require(a != null)
        x<!UNSAFE_CALL!>.<!>length
        require(b != null)
        x.length
    }
    a = ""
    f()
}

fun testRequireNotNullLambda(x: String?) {
    var a = x
    val b = a
    callLambdaWithoutContract { a = "" }
    require(a != null)
    x<!UNSAFE_CALL!>.<!>length
    require(b != null)
    x.length
}

fun testNotNullViaVariable(x: String?) {
    val a = x != null
    var b = a
    val c = b
    if (b) x.length
    if (c) x.length
    val f = {
        if (b) x<!UNSAFE_CALL!>.<!>length
        if (c) x.length
    }
    b = true
    f()
}

fun testNotNullViaVariableLambda(x: String?) {
    val a = x != null
    var b = a
    val c = b
    if (b) x.length
    if (c) x.length
    callLambdaWithoutContract { b = true }
    if (b) x<!UNSAFE_CALL!>.<!>length
    if (c) x.length
}

fun testRequireNotNullViaVariable(x: String?) {
    val a = x != null
    var b = a
    val c = b
    val f: () -> Unit = {
        require(b)
        x<!UNSAFE_CALL!>.<!>length
        require(c)
        x.length
    }
    b = true
    f()
}

fun testRequireNotNullViaVariableLambda(x: String?) {
    val a = x != null
    var b = a
    val c = b
    callLambdaWithoutContract { b = true }
    require(b)
    x<!UNSAFE_CALL!>.<!>length
    require(c)
    x.length
}
