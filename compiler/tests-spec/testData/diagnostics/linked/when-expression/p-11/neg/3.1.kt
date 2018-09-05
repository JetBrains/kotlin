// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [3] The bound expression is of type kotlin.Boolean and the conditions contain both:
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive 'when' when not contains by all Boolean values or 'when' does not have bound value.
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

// CASE DESCRIPTION: Checking for not exhaustive 'when' without bound value on the Boolean.
fun case_4(value: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value == true -> ""
    value == false -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' with both Boolean values covered, but using variables.
 DISCUSSION: maybe use const propagation here?
 ISSUES: KT-25265
 */
fun case_5(value: Boolean): String {
    val trueValue = true
    val falseValue = false

    return <!NO_ELSE_IN_WHEN!>when<!> (value) {
        trueValue -> ""
        falseValue -> ""
    }
}
