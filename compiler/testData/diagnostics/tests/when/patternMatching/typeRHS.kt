data class Pair<F, S>(val first: F, val second: S)

fun foo1(x: Any?) = when (x) {
    is <!NO_TYPE_ARGUMENTS_ON_RHS!>Pair<!> -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<*, *><!> -> 0
    is <!CANNOT_CHECK_FOR_ERASED!>Pair<Int, Int><!> -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>() -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<*, *><!>() -> 0
    is <!CANNOT_CHECK_FOR_ERASED, DUPLICATE_LABEL_IN_WHEN!>Pair<Int, Int><!>() -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!NO_TYPE_ARGUMENTS_ON_RHS!>Pair<!>) -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: Pair<*, *>) -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!CANNOT_CHECK_FOR_ERASED!>Pair<Int, Int><!>) -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = Pair()) -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = Pair<*, *>()) -> 0
    is <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = <!CANNOT_CHECK_FOR_ERASED!>Pair<Int, Int><!>()) -> 0
    else -> 0
}

fun foo2(x: Pair<Pair<Int, Int>, Any?>) = when (x) {
    is <!USELESS_IS_CHECK!>Pair<!> -> 0
    is <!USELESS_IS_CHECK!>Pair<*, *><!> -> 0
    is <!CANNOT_CHECK_FOR_ERASED!>Pair<Int, Int><!> -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>() -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<*, *><!>() -> 0
    is <!CANNOT_CHECK_FOR_ERASED, DUPLICATE_LABEL_IN_WHEN!>Pair<Int, Int><!>() -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!USELESS_IS_CHECK!>Pair<!>) -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!USELESS_IS_CHECK!>Pair<*, *><!>) -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!>: <!USELESS_IS_CHECK!>Pair<Int, Int><!>) -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = <!USELESS_IS_CHECK!>Pair<!>()) -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = <!USELESS_IS_CHECK!>Pair<*, *><!>()) -> 0
    is <!USELESS_IS_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> = <!USELESS_IS_CHECK!>Pair<Int, Int><!>()) -> 0
    else -> 0
}