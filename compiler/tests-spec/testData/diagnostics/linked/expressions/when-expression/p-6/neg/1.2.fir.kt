// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and type test condition on the non-type operand of the type checking operator.
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any, value_2: Int): String {
    when (value_1) {
        is <!UNRESOLVED_REFERENCE!>value_2<!> -> return ""
    }

    return ""
}
