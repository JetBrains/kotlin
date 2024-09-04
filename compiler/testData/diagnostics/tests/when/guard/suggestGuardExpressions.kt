// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS

fun ok(x: String, boolExpr: Boolean) {
    when (x) {
        "OK" <!UNSUPPORTED_FEATURE!>if boolExpr<!> -> "hello"
        else -> "bye"
    }
}

fun suggestAnd(x: String, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR, INCOMPATIBLE_TYPES!><!TYPE_MISMATCH!>"OK"<!> && boolExpr<!> -> "hello"
    }
}

fun booleanNoSuggestion1(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!><!TYPE_MISMATCH!>"OK"<!> && boolExpr<!> -> "hello"
        else -> "bye"
    }
}

fun booleanNoSuggestion2(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>true && boolExpr<!> -> "hello"
        else -> "bye"
    }
}

fun elseOk(x: Boolean, boolExpr: Boolean) {
    when (x) {
        else <!UNSUPPORTED_FEATURE!>if boolExpr<!> -> "hello"
        else -> "bye"
    }
}

fun elseAndAnd(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> <!SYNTAX!>&& boolExpr<!> -> "hello"
        <!UNREACHABLE_CODE!>else -> "bye"<!>
    }
}

fun comma(x: Any, boolExpr: Boolean) {
    when (x) {
        is String, is CharSequence <!UNSUPPORTED_FEATURE!>&& boolExpr<!> -> "hello"
    }
}

fun ifButNotGuard1(x: Any) {
    when (x) {
        <!INVALID_IF_AS_EXPRESSION!>if<!><!SYNTAX!><!> x == "1" -> 1
    }
}

fun ifButNotGuard2(x: Any) {
    when (x) {
        if (x == "1") "ok" else "wrong" -> 1
    }
}
