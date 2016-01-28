// See KT-8277

val v = { true } <!USELESS_ELVIS!><!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> ( { true } <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!! )<!>

val w = if (true) {
    { true }
}
else {
    { true } <!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> null!!
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>ww<!> = if (true) {
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

val bbb = null ?: ( l() ?: null)

val bbbb = ( l() ?: null) ?: ( l() ?: null)

fun f(x : Long?): Long {
    var a = x ?: (<!TYPE_MISMATCH!>fun() {}<!> <!USELESS_ELVIS!><!USELESS_ELVIS_ON_LAMBDA_EXPRESSION!>?:<!> <!TYPE_MISMATCH!>fun() {}<!><!>)
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
}
