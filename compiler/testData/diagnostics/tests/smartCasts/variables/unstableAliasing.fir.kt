// WITH_STDLIB

fun callLambdaWithoutContract(block: () -> Unit) = block()

class AnyHolder(val value: Any?)
class StringHolder(val value: String?)

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

fun testIs(x: AnyHolder) {
    var a = x
    val b = a
    if (a.value is String) x.value.length
    if (b.value is String) x.value.length
    val f = {
        if (a.value is String) x.value.<!UNRESOLVED_REFERENCE!>length<!>
        if (b.value is String) x.value.length
    }
    a = AnyHolder("")
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

fun testIsLambda(x: AnyHolder) {
    var a = x
    val b = a
    if (a.value is String) x.value.length
    if (b.value is String) x.value.length
    callLambdaWithoutContract { a = AnyHolder("") }
    if (a.value is String) x.value.<!UNRESOLVED_REFERENCE!>length<!>
    if (b.value is String) x.value.length
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

fun testRequireIs(x: AnyHolder) {
    var a = x
    val b = a
    val f: () -> Unit = {
        require(a.value is String)
        x.value.<!UNRESOLVED_REFERENCE!>length<!>
        require(b.value is String)
        x.value.length
    }
    a = AnyHolder("")
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

fun testRequireIsLambda(x: AnyHolder) {
    var a = x
    val b = a
    callLambdaWithoutContract { a = AnyHolder("") }
    require(a.value is String)
    x.value.<!UNRESOLVED_REFERENCE!>length<!>
    require(b.value is String)
    x.value.length
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

fun testNotNull(x: StringHolder) {
    var a = x
    val b = a
    if (a.value != null) x.value.length
    if (b.value != null) x.value.length
    val f = {
        if (a.value != null) x.value<!UNSAFE_CALL!>.<!>length
        if (b.value != null) x.value.length
    }
    a = StringHolder("")
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

fun testNotNullLambda(x: StringHolder) {
    var a = x
    val b = a
    if (a.value != null) x.value.length
    if (b.value != null) x.value.length
    callLambdaWithoutContract { a = StringHolder("") }
    if (a.value != null) x.value<!UNSAFE_CALL!>.<!>length
    if (b.value != null) x.value.length
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

fun testRequireNotNull(x: StringHolder) {
    var a = x
    val b = a
    val f: () -> Unit = {
        require(a.value != null)
        x.value<!UNSAFE_CALL!>.<!>length
        require(b.value != null)
        x.value.length
    }
    a = StringHolder("")
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

fun testRequireNotNullLambda(x: StringHolder) {
    var a = x
    val b = a
    callLambdaWithoutContract { a = StringHolder("") }
    require(a.value != null)
    x.value<!UNSAFE_CALL!>.<!>length
    require(b.value != null)
    x.value.length
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

fun testNotNullViaVariable(x: StringHolder) {
    val a = x.value != null
    var b = a
    val c = b
    if (b) x.value.length
    if (c) x.value.length
    val f = {
        if (b) x.value<!UNSAFE_CALL!>.<!>length
        if (c) x.value.length
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

fun testNotNullViaVariableLambda(x: StringHolder) {
    val a = x.value != null
    var b = a
    val c = b
    if (b) x.value.length
    if (c) x.value.length
    callLambdaWithoutContract { b = true }
    if (b) x.value<!UNSAFE_CALL!>.<!>length
    if (c) x.value.length
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

fun testRequireNotNullViaVariable(x: StringHolder) {
    val a = x.value != null
    var b = a
    val c = b
    val f: () -> Unit = {
        require(b)
        x.value<!UNSAFE_CALL!>.<!>length
        require(c)
        x.value.length
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

fun testRequireNotNullViaVariableLambda(x: StringHolder) {
    val a = x.value != null
    var b = a
    val c = b
    callLambdaWithoutContract { b = true }
    require(b)
    x.value<!UNSAFE_CALL!>.<!>length
    require(c)
    x.value.length
}
