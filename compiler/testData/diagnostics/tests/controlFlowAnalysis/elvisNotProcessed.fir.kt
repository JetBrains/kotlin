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
    { true } <!USELESS_ELVIS!>?: null!!<!>
}
else if (true) {
    { true } <!USELESS_ELVIS!>?: null!!<!>
}
else {
    null!!
}

val n = null ?: (null ?: { true })

fun l(): (() -> Boolean)? = null

val b = null ?: ( l() ?: false)

val bb = null ?: ( l() ?: null!!)

val bbb = null ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

val bbbb = ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>) ?: ( l() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)

fun f(x : Long?): Long {
    var a = x ?: (fun() {} <!USELESS_ELVIS!>?: fun() {}<!>)
    return <!RETURN_TYPE_MISMATCH!>a<!>
}
