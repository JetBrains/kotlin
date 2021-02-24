// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    private fun foo(i: Int) {}
    private fun foo(s: String) {}
}

fun test(a: A) {
    a.<!NONE_APPLICABLE!>foo<!>(3)
    a.<!NONE_APPLICABLE!>foo<!>()
}

