class IncDec {
    operator fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = <!RESULT_TYPE_MISMATCH!>x++<!>
    <!RESULT_TYPE_MISMATCH!>x++<!>
    return x
}
