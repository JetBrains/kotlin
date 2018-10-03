// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the areas above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive 'when' on the nullable Boolean.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean without null-check branch.
fun case_1(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    false -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean with null-check branch, but all possible values not covered.
fun case_2(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean without branches.
fun case_3(value_1: Boolean?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }
