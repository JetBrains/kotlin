// !WITH_NEW_INFERENCE
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
    <!TYPE_MISMATCH{OI}!>{ true }<!> <!USELESS_ELVIS!>?: null!!<!>
}
else if (true) {
    <!TYPE_MISMATCH{OI}!>{ true }<!> <!USELESS_ELVIS!>?: null!!<!>
}
else {
    null!!
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE{OI}!>n<!> = null ?: (null ?: <!TYPE_MISMATCH{OI}!>{ true }<!>)

fun l(): (() -> Boolean)? = null

val b = null ?: ( l() ?: false)

val bb = null ?: ( l() ?: null!!)

val bbb = null ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

val bbbb = ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>) ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

fun f(x : Long?): Long {
    var a = x ?: (<!TYPE_MISMATCH{OI}!>fun() {}<!> <!USELESS_ELVIS!>?: <!TYPE_MISMATCH{OI}!>fun() {}<!><!>)
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, TYPE_MISMATCH{NI}!>a<!>
}
