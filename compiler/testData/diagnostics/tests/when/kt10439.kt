// !WITH_NEW_INFERENCE
fun foo(x: Int) = x

fun test0(flag: Boolean) {
    foo(<!NI;TYPE_MISMATCH!>if (flag) <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!> else <!OI;TYPE_MISMATCH!>""<!><!>)
}

fun test1(flag: Boolean) {
    foo(<!NI;TYPE_MISMATCH!>when (flag) {
        true -> <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>
        else -> <!OI;TYPE_MISMATCH!>""<!>
    }<!>)
}
