// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: T) = t

fun test1() {
    <!INAPPLICABLE_CANDIDATE!>foo<!><Int, String>("")
}


fun <T, R> bar(t: T, r: R) {}

fun test2() {
    <!INAPPLICABLE_CANDIDATE!>bar<!><Int>("", "")
}