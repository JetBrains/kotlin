// !WITH_NEW_INFERENCE
// See KT-8277

val v = { true } <!USELESS_ELVIS!>?: ( { true } <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!! )<!>

val w = if (true) {
    { true }
}
else {
    { true } <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!!
}

val ww = if (true) {
    <!TYPE_MISMATCH!>{ true }<!> <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!!
}
else if (true) {
    <!TYPE_MISMATCH!>{ true }<!> <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!!
}
else {
    null!!
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>n<!> = null ?: (null ?: <!TYPE_MISMATCH!>{ true }<!>)

fun l(): (() -> Boolean)? = null

val b = null ?: ( l() ?: false)

val bb = null ?: ( l() ?: null!!)

val bbb = null ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

val bbbb = ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>) ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

fun f(x : Long?): Long {
    var a = x ?: (<!TYPE_MISMATCH!>fun() {}<!> <!USELESS_ELVIS!>?: <!TYPE_MISMATCH!>fun() {}<!><!>)
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
}
