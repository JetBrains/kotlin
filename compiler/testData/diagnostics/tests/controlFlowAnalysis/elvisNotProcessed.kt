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
    <!OI;TYPE_MISMATCH!>{ true }<!> <!USELESS_ELVIS!>?: null!!<!>
}
else if (true) {
    <!OI;TYPE_MISMATCH!>{ true }<!> <!USELESS_ELVIS!>?: null!!<!>
}
else {
    null!!
}

val <!OI;IMPLICIT_NOTHING_PROPERTY_TYPE!>n<!> = null ?: (null ?: <!OI;TYPE_MISMATCH!>{ true }<!>)

fun l(): (() -> Boolean)? = null

val b = null ?: ( l() ?: false)

val bb = null ?: ( l() ?: null!!)

val bbb = null ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

val bbbb = ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>) ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

fun f(x : Long?): Long {
    var a = x ?: (<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>fun() {}<!> <!USELESS_ELVIS!>?: <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>fun() {}<!><!>)
    return <!OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
}
