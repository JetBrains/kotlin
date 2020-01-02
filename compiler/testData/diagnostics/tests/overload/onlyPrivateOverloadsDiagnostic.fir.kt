// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    private fun foo(i: Int) {}
    private fun foo(s: String) {}
}

fun test(a: A) {
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>(3)
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>()
}

