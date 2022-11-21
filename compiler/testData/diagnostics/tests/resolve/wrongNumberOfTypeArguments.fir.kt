// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: T) = t

fun test1() {
    <!INAPPLICABLE_CANDIDATE!>foo<!><Int, String>(0)
}


fun <T, R> bar(t: T, r: R) {}

fun test2() {
    <!INAPPLICABLE_CANDIDATE!>bar<!><Int>(0, "")
}
