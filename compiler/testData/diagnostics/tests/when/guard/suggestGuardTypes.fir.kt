// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS
// FIR_IDENTICAL

fun ok(x: Any, boolExpr: Boolean) {
    when (x) {
        is String if boolExpr -> "hello"
    }
}

fun wrongAnd(x: Any, boolExpr: Boolean) {
    when (x) {
        is String <!INCORRECT_GUARD_KEYWORD!>&&<!> boolExpr -> "hello"
    }
}

fun comma(x: Any, boolExpr: Boolean) {
    when (x) {
        is String, is Int <!COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD!>&& boolExpr<!> -> "hello"
    }
}
