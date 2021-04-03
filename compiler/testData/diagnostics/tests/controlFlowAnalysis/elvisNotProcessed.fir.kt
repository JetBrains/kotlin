// !WITH_NEW_INFERENCE
// See KT-8277
// NI_EXPECTED_FILE

val v = { true } ?: ( { true } ?:null!! )

val w = if (true) {
    { true }
}
else {
    { true } ?: null!!
}

val ww = if (true) {
    { true } ?: null!!
}
else if (true) {
    { true } ?: null!!
}
else {
    null!!
}

val n = null ?: (null ?: { true })

fun l(): (() -> Boolean)? = null

val b = null ?: ( l() ?: false)

val bb = null ?: ( l() ?: null!!)

val bbb = null ?: ( l() ?: null)

val bbbb = ( l() ?: null) ?: ( l() ?: null)

fun f(x : Long?): Long {
    var a = x ?: (fun() {} ?: fun() {})
    return <!RETURN_TYPE_MISMATCH!>a<!>
}
