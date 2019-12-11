// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

fun foo() {}
fun foo(s: Int) {}


fun bar(a: Any) {}
fun bar(a: Int) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, 2)
    <!INAPPLICABLE_CANDIDATE!>foo<!>("")

    <!INAPPLICABLE_CANDIDATE!>bar<!>(1, 2)
    <!INAPPLICABLE_CANDIDATE!>bar<!>()
}