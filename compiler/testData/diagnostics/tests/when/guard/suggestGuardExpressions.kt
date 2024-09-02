// LANGUAGE: +WhenGuards
// WITH_EXTENDED_CHECKERS
// FIR_IDENTICAL

fun foo(x: String, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR, INCOMPATIBLE_TYPES!><!CONDITION_TYPE_MISMATCH!>"OK"<!> <!SUGGEST_GUARD_KEYWORD!>&&<!> boolExpr<!> -> "hello"
    }
}

fun bar(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!><!CONDITION_TYPE_MISMATCH!>"OK"<!> && boolExpr<!> -> "hello"
        else -> "bye"
    }
}

fun baz(x: Boolean, boolExpr: Boolean) {
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>true && boolExpr<!> -> "hello"
        else -> "bye"
    }
}
