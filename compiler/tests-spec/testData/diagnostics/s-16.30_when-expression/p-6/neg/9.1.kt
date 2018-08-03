// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !CHECK_TYPE

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 9: The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when contains by all Boolean values, bot no null check (or with no null check, but not contains by all Boolean values).
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean without null-check branch.
fun case_1(value: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    true -> ""
    false -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean with null-check branch, but all possible values not covered.
fun case_2(value: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    true -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean without branches.
fun case_3(value: Boolean?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }
