data class Pair<F, S>(val first: F, val second: S)

fun foo1(x: Any?) = when (x) {
    is Pair(val a, val b) -> 0
    is <!USELESS_IS_CHECK!>Pair<!> -> 0
    is <!USELESS_IS_CHECK!>Pair<!>(val a: Int, val b: Int) -> 0
    is (val a: Int, val b: Int) -> 0
    else -> 0
}

fun foo2(x: Pair<Int, Int>) = when (x) {
    is <!USELESS_IS_CHECK!>Pair<!> -> 0
    is <!USELESS_IS_CHECK!>Pair<!>(val a: <!USELESS_IS_CHECK!>Int<!>, val b: <!USELESS_IS_CHECK!>Int<!>) -> 0
    is (val a: <!USELESS_IS_CHECK!>Int<!>, val b: <!USELESS_IS_CHECK!>Int<!>) -> 0
    <!USELESS_ELSE_STATEMENT!>else<!> -> 0
}

fun foo2(x: Pair<Int, Int>) = when (x) {
    is (val a, val b) -> 0
    <!USELESS_ELSE_STATEMENT!>else<!> -> 0
}