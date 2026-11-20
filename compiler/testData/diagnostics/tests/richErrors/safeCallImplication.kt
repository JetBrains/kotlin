// RUN_PIPELINE_TILL: FRONTEND
error class Foo

fun test(x: String | Foo) {
    val y = x|.length
    if (y is NonError?) {
        x[0]
    }
    if (y is Int) {
        x[0]
    }
    if (y !is RichError) {
        x[0]
    }
    if (y !is Foo) {
        <!UNSAFE_CALL!>x[0]<!>
    }
    if (y is RichError) {
        <!UNSAFE_CALL!>x[0]<!>
    }
    if (y is Foo) {
        <!UNSAFE_CALL!>x[0]<!>
    }
}

fun testCast1(x: String | Foo) {
    val y = x|.length
    y as NonError?
    x[0]
}

fun testCast2(x: String | Foo) {
    val y = x|.length
    y as Int
    x[0]
}

fun testCast3(x: String | Foo) {
    val y = x|.length
    y as RichError
    <!UNSAFE_CALL!>x[0]<!>
}

fun testCast4(x: String | Foo) {
    val y = x|.length
    y as Foo
    <!UNSAFE_CALL!>x[0]<!>
}

fun testSafeCast(x: String | Foo) {
    val y = x|.length
    if ((y as? NonError?) != null) {
        x[0]
    }
    if ((y as? Int) != null) {
        x[0]
    }
    if ((y as? RichError) == null) {
        x[0]
    }
    if ((y as? Foo) == null) {
        <!UNSAFE_CALL!>x[0]<!>
    }
    if ((y as? RichError) != null) {
        <!UNSAFE_CALL!>x[0]<!>
    }
    if ((y as? Foo) != null) {
        <!UNSAFE_CALL!>x[0]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, isExpression, localProperty,
propertyDeclaration, safeCall */
