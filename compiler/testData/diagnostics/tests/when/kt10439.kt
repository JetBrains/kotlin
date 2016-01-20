fun foo(x: Int) = x

fun test0(flag: Boolean) {
    foo(if (flag) <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!> else <!TYPE_MISMATCH!>""<!>)
}

fun test1(flag: Boolean) {
    foo(when (flag) {
        true -> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>
        else -> <!TYPE_MISMATCH!>""<!>
    })
}
