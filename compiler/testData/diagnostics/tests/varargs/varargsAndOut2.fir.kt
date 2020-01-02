// !DIAGNOSTICS: -UNUSED_PARAMETER

fun bar(vararg a: String) {}

fun test2(a: Array<String>, b: Array<out String>) {
    bar(*a)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(*b)
    <!INAPPLICABLE_CANDIDATE!>bar<!>("", *a, *b, "")
}