// LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Non-exhaustive when using boolean values.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    false -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }

// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value_1 == true -> ""
    value_1 == false -> ""
}

/*
 * TESTCASE NUMBER: 5
 * DISCUSSION: maybe use const propagation here?
 * ISSUES: KT-25265
 */
fun case_5(value_1: Boolean): String {
    val trueValue = true
    val falseValue = false

    return <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
        trueValue -> ""
        falseValue -> ""
    }
}


// TESTCASE NUMBER: 6
fun case_6(value_1: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    <!CONFUSING_BRANCH_CONDITION_ERROR!>true && false && ((true || false)) || true && !!!false && !!!true<!> -> ""
    <!CONFUSING_BRANCH_CONDITION_ERROR!>true && false && ((true || false)) || true && !!!false<!> -> ""
}
