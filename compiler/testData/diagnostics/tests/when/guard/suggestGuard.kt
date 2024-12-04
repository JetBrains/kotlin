// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS

fun suggestAnd(x: String, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR, INCOMPATIBLE_TYPES!><!TYPE_MISMATCH!>"OK"<!> && boolExpr<!> -> "hello"
    }
}

fun ok(x: String, boolExpr: Boolean) {
    when (x) {
        "OK" <!UNSUPPORTED_FEATURE!>if boolExpr<!> -> "hello"
        else -> "bye"
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
