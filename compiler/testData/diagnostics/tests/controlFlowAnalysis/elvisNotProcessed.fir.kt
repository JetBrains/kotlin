// RUN_PIPELINE_TILL: FRONTEND
// See KT-8277
// NI_EXPECTED_FILE

val v = { true } <!USELESS_ELVIS!>?: ( { true } <!USELESS_ELVIS!>?:null!!<!> )<!>

val w = if (true) {
    { true }
}
else {
    { true } <!USELESS_ELVIS!>?: null!!<!>
}

val ww = if (true) {
    { true } <!USELESS_ELVIS!>?: null!!<!>
}
else if (true) {
    { true } <!USELESS_ELVIS!>?: null!!<!>
}
else {
    null!!
}

val n = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> (<!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> { true })

fun l(): (() -> Boolean)? = null

val b = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> ( l() ?: false)

val bb = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> ( l() ?: null!!)

val bbb = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

val bbbb = ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>) ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

fun f(x : Long?): Long {
    var a = x ?: (fun() {} <!USELESS_ELVIS!>?: fun() {}<!>)
    return <!RETURN_TYPE_MISMATCH!>a<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, checkNotNullCall, elvisExpression, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration */
