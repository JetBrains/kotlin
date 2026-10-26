// RUN_PIPELINE_TILL: FRONTEND
error class Foo

fun testSimple(x: String | Foo) {
    if (x is String) x.length
    if (x is NonError) x.length
    if (x is NonError?) x.length
    if (x !is RichError) x.length
    if (x !is RichError?) x.length
    if (x !is Foo) x<!UNSAFE_CALL!>.<!>length
    if (x !is NonError?) { val y: RichError = x }
    if (x !is NonError?) { val y: Foo = <!INITIALIZER_TYPE_MISMATCH!>x<!> }
}

fun testSimpleAs1(x: String | Foo) {
    x as String
    x.length
}

fun testSimpleAs2(x: String | Foo) {
    x as NonError?
    x.length
}

fun testSimpleAs3(x: String | Foo) {
    x as NonError
    x.length
}

fun testNullable(x: String? | Foo) {
    if (x is String) x.length
    if (x is NonError) x.length
    if (x is NonError?) x<!UNSAFE_CALL!>.<!>length
    if (x !is RichError) x<!UNSAFE_CALL!>.<!>length
    if (x !is RichError?) x.length
    if (x !is Foo) x<!UNSAFE_CALL!>.<!>length
    if (x !is NonError?) { val y: RichError = x }
}

fun <T : CharSequence | Foo> testTypeParameter(x: T) {
    if (x is String) x.length
    if (x is NonError) x.length
    if (x is NonError?) x.length
    if (x !is RichError) x.length
    if (x !is RichError?) x.length
    if (x !is Foo) x<!UNSAFE_CALL!>.<!>length
    if (x !is NonError?) { val y: RichError = x }
}

fun <T : CharSequence | Foo> testTypeParameterAs1(x: T) {
    x as String
    x.length
}

fun <T : CharSequence | Foo> testTypeParameterAs2(x: T) {
    x as NonError?
    x.length
}

fun <T : CharSequence | Foo> testTypeParameterAs3(x: T) {
    x as NonError
    x.length
}

fun <T : CharSequence? | Foo> testTypeParameterNullable(x: T) {
    if (x is String) x.length
    if (x is NonError) x.length
    if (x is NonError?) x<!UNSAFE_CALL!>.<!>length
    if (x !is RichError) x<!UNSAFE_CALL!>.<!>length
    if (x !is RichError?) x.length
    if (x !is Foo) x<!UNSAFE_CALL!>.<!>length
    if (x !is NonError?) { val y: RichError = x }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, smartcast */
