// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

fun foo() {}
fun foo(s: Int) {}


fun bar(a: Any) {}
fun bar(a: Int) {}

fun test() {
    <!NONE_APPLICABLE!>foo<!>(1, 2)
    foo(<!TYPE_MISMATCH!>""<!>)

    <!OI;NONE_APPLICABLE!>bar<!>(1, <!NI;TOO_MANY_ARGUMENTS!>2<!>)
    <!NONE_APPLICABLE!>bar<!>()
}