// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS

fun ok(x: String, boolExpr: Boolean) {
    when (x) {
        "OK" if boolExpr -> "hello"
        else -> "bye"
    }
}

fun suggestAnd(x: String, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR, INCOMPATIBLE_TYPES!><!CONDITION_TYPE_MISMATCH!>"OK"<!> <!SUGGEST_GUARD_KEYWORD!>&&<!> boolExpr<!> -> "hello"
    }
}

fun booleanNoSuggestion1(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!><!CONDITION_TYPE_MISMATCH!>"OK"<!> && boolExpr<!> -> "hello"
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
        else if boolExpr -> "hello"
        else -> "bye"
    }
}

fun elseAndAnd(x: Boolean, boolExpr: Boolean) {
    when (x) {
        else <!INCORRECT_GUARD_KEYWORD!>&&<!> boolExpr -> "hello"
        else -> "bye"
    }
}

fun comma(x: Any, boolExpr: Boolean) {
    when (x) {
        is String, is CharSequence <!COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD!>&& boolExpr<!> -> "hello"
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
