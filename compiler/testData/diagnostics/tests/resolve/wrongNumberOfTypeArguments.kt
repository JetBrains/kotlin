// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: T) = t

fun test1() {
    foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>(0)
}


fun <T, R> bar(t: T, r: R) {}

fun test2() {
    bar<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(0, "")
}
