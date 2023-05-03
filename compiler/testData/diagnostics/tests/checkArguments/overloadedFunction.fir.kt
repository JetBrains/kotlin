// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

fun foo() {}
fun foo(s: Int) {}


fun bar(a: Any) {}
fun bar(a: Int) {}

fun test() {
    <!NONE_APPLICABLE!>foo<!>(1, 2)
    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)

    bar(1, <!TOO_MANY_ARGUMENTS!>2<!>)
    <!NONE_APPLICABLE!>bar<!>()
}
