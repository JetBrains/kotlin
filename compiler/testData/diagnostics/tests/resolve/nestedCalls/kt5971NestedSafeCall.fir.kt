// !DIAGNOSTICS: -UNUSED_PARAMETER

//KT-5971 Missing error when fun argument is safe call

fun foo(i: Int) {}

fun test(s: String?) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s?.length)
}
