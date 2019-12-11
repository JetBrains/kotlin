// !WITH_NEW_INFERENCE
fun foo(x: Int) = x

fun test0(flag: Boolean) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(if (flag) true else "")
}

fun test1(flag: Boolean) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(when (flag) {
        true -> true
        else -> ""
    })
}
