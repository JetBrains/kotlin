// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <reified T> foo(t: T) {}
class C<reified T>(t: T)

fun test(d: dynamic) {
    foo<<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>dynamic<!>>(d)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!>(d)

    C<<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>dynamic<!>>(d)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>C<!>(d)
}