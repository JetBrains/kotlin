// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS
// FIR_IDENTICAL

fun foo(x: Any, boolExpr: Boolean) {
    when (x) {
        is String if boolExpr -> "hello"
    }
}

fun bar(x: Any, boolExpr: Boolean) {
    when (x) {
        is String <!INCORRECT_GUARD_KEYWORD!>&&<!> boolExpr -> "hello"
    }
}
