// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <reified T> foo(t: T) {}
class C<reified T>(t: T)

fun test(d: dynamic) {
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!><dynamic>(d)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!>(d)

    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>C<!><dynamic>(d)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>C<!>(d)
}