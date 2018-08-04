// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 11
 SENTENCE 3: The bound expression is of type kotlin.Boolean and the conditions contain both:
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when not contains by all Boolean values.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean value (with only true branch).
fun case_1(value: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    true -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean value (with only false branch).
fun case_2(value: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    false -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Boolean value (no branches).
fun case_3(value: Boolean): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }
