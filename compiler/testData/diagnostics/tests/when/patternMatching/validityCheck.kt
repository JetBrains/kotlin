data class Pair<F, S>(val first: F, val second: S)

fun foo1(x: Any?) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Pair(val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
    is <!NO_TYPE_ARGUMENTS_ON_RHS, DUPLICATE_LABEL_IN_WHEN!>Pair<!> -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: Int, val <!UNUSED_VARIABLE!>b<!>: Int) -> 0
}

fun foo2(x: Pair<Int, Int>) = when (x) {
    is <!USELESS_IS_CHECK!>Pair<!> -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!USELESS_IS_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!>: <!USELESS_IS_CHECK!>Int<!>) -> 0
    is (val <!UNUSED_VARIABLE!>a<!>: <!USELESS_IS_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!>: <!USELESS_IS_CHECK!>Int<!>) -> 0
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 0
}

fun foo3(x: Pair<Int, Int>) = when (x) {
    is (val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 0
}
